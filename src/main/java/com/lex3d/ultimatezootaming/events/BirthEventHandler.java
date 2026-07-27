package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.core.network.BirthToastS2CPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * "Quand un bebe nait de parents dont l'un est tame, une Toast Notification s'affiche."
 * La touche [K] ouvre ensuite le MaternityScreen pour nommer/adopter (voir ClientSetup).
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class BirthEventHandler {

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getParentA().level().isClientSide()) return;

        AgeableMob child = event.getChild();
        if (child == null) return;

        // Recupere l'UUID du proprietaire (via taming natif OU notre capability)
        java.util.UUID ownerId = findOwnerUUID(event.getParentA());
        if (ownerId == null) ownerId = findOwnerUUID(event.getParentB());
        if (ownerId == null) return;

        // --- Genetique legere : le bebe herite d'un trait d'un parent ---
        inheritTraits(event.getParentA(), event.getParentB(), child, ownerId);

        // Cherche le joueur sur TOUT le serveur (pas seulement a proximite) : les
        // naissances se produisent souvent dans un enclos loin du joueur.
        if (!(event.getParentA().level().getServer() != null)) return;
        // Multi-zoo : previent TOUS les joueurs en ligne
        for (ServerPlayer p : event.getParentA().level().getServer().getPlayerList().getPlayers()) {
            notifyBirth(p, child);
            // Le toast "appuie sur K" pour nommer/adopter le nouveau-ne
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new BirthToastS2CPacket(child.getUUID(), child.getType().getDescriptionId()));
        }
    }

    /**
     * Le bebe herite d'un trait : 40% de chance de prendre le trait d'un parent
     * (au hasard entre les deux), sinon reste ordinaire. Le bebe est aussi
     * automatiquement apprivoise au meme proprietaire (il naît dans le zoo).
     */
    private static void inheritTraits(Mob parentA, Mob parentB, AgeableMob child, java.util.UUID ownerId) {
        com.lex3d.ultimatezootaming.capability.TamingData.Trait traitA = traitOf(parentA);
        com.lex3d.ultimatezootaming.capability.TamingData.Trait traitB = traitOf(parentB);

        // L'enclos des parents : le bebe y est assigne automatiquement
        var dataA = parentA.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        java.util.UUID parentZone = dataA != null ? dataA.getZoneId() : null;
        if (parentZone == null) {
            var dataB = parentB.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
            parentZone = dataB != null ? dataB.getZoneId() : null;
        }
        var dataB2 = parentB.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        var babyData = child.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (babyData == null) { child.setPersistenceRequired(); return; }

        // Le bebe appartient au proprietaire des parents
        babyData.setOwnerUUID(ownerId);
        babyData.setForcedTame(true);
        babyData.setTrust(100f);
        if (parentZone != null) babyData.setZoneId(parentZone); // ne dans l'enclos = assigne

        // ---- MEME HERITAGE QUE L'INCUBATEUR (reproduction naturelle aux croquettes) ----
        java.util.Random rng = new java.util.Random();

        // Jour de naissance
        if (parentA.level() instanceof net.minecraft.server.level.ServerLevel bsl) {
            babyData.setCaptureDay(
                    com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(bsl).getDay());
        }

        // Lignee : parents + generation
        babyData.setParents(parentA.getUUID(), parentB.getUUID());
        int genA = dataA != null ? dataA.getGeneration() : 0;
        int genB = dataB2 != null ? dataB2.getGeneration() : 0;
        babyData.setGeneration(Math.max(genA, genB) + 1);

        // Trait : 60% herite d'un parent, 30% aleatoire, 10% mutation
        float r = rng.nextFloat();
        boolean mutated = false;
        var traits = com.lex3d.ultimatezootaming.capability.TamingData.Trait.values();
        if (r < 0.60f) {
            var chosen = rng.nextBoolean() ? traitA : traitB;
            // Si le parent tire n'a pas de trait, on prend celui de l'autre s'il en a un
            if (chosen == com.lex3d.ultimatezootaming.capability.TamingData.Trait.NONE) {
                chosen = (traitA != com.lex3d.ultimatezootaming.capability.TamingData.Trait.NONE)
                        ? traitA : traitB;
            }
            babyData.setTrait(chosen);
        } else if (r < 0.90f) {
            babyData.setTrait(traits[rng.nextInt(traits.length)]);
        } else {
            // MUTATION : trait special garanti + rarete augmentee
            babyData.setTrait(traits[1 + rng.nextInt(traits.length - 1)]);
            mutated = true;
        }

        // Rarete : la meilleure des deux parents, +1 si mutation (plafond 3)
        int rarA = dataA != null ? dataA.getRarity() : 0;
        int rarB = dataB2 != null ? dataB2.getRarity() : 0;
        int inherited = Math.max(rarA, rarB);
        babyData.setRarity(Math.min(3, mutated ? inherited + 1 : inherited));

        // Historique des parents + meilleurs amis
        if (dataA != null) {
            dataA.addBabyCount();
            if (dataA.getBestFriend() == null) dataA.setBestFriend(parentB.getUUID());
        }
        if (dataB2 != null) {
            dataB2.addBabyCount();
            if (dataB2.getBestFriend() == null) dataB2.setBestFriend(parentA.getUUID());
        }

        // Une mutation rare merite une annonce
        if (mutated && babyData.getRarity() > 0
                && parentA.level().getServer() != null) {
            for (ServerPlayer p : parentA.level().getServer().getPlayerList().getPlayers()) {
                p.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u2726 ")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                        .append(net.minecraft.network.chat.Component.translatable(
                                "message.ultimatezootaming.mutation_born",
                                net.minecraft.network.chat.Component.translatable(
                                        child.getType().getDescriptionId()),
                                net.minecraft.network.chat.Component.translatable(
                                        "gui.ultimatezootaming.rarity." + babyData.getRarity()))
                                .withStyle(net.minecraft.ChatFormatting.WHITE)));
            }
        }

        child.setPersistenceRequired();
    }

    private static com.lex3d.ultimatezootaming.capability.TamingData.Trait traitOf(Mob parent) {
        return parent.getCapability(CapabilityHandler.TAMING_DATA).resolve()
                .map(com.lex3d.ultimatezootaming.capability.TamingData::getTrait)
                .orElse(com.lex3d.ultimatezootaming.capability.TamingData.Trait.NONE);
    }

    private static void notifyBirth(ServerPlayer p, AgeableMob child) {
        if (p.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            // Une naissance fait le buzz : affluence boostee pendant 1 jour MC
            com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl).setHype(24000);
        }
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal("\ud83d\udc23 ")
                .append(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.birth",
                        net.minecraft.network.chat.Component.translatable(child.getType().getDescriptionId()))
                        .withStyle(net.minecraft.ChatFormatting.GREEN))
                .append(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.birth_hype")
                        .withStyle(net.minecraft.ChatFormatting.GOLD)));
        p.level().playSound(null, p.blockPosition(),
                net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.6f);
    }

    private static java.util.UUID findOwnerUUID(Mob parent) {
        if (parent instanceof TamableAnimal tamable && tamable.getOwnerUUID() != null) {
            return tamable.getOwnerUUID();
        }
        final java.util.UUID[] result = {null};
        parent.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            if (data.isTamed()) result[0] = data.getOwnerUUID();
        });
        return result[0];
    }
}
