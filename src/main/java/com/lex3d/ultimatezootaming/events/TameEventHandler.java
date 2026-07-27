package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.core.init.ModSounds;
import com.lex3d.ultimatezootaming.config.ConfigSyncManager;
import com.lex3d.ultimatezootaming.items.KibbleItem;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Gere TOUS les clics-droits sur les mobs :
 *
 * 1. MAIN VIDE sur un familier tame -> toggle Assis/Debout instantane (pas besoin
 *    d'ouvrir le GUI du Sifflet pour ca).
 * 2. CROQUETTE sur un mob tame -> le met "in love" pour la REPRODUCTION (si la
 *    croquette correspond a son regime), comme le ble sur une vache vanilla.
 * 3. CROQUETTE sur un mob sauvage -> tentative de taming Croquettes+RNG, avec la
 *    Regle d'or (Compatibilite Douce) : si le mob a un taming natif (TamableAnimal)
 *    ET que son mod n'est pas dans la Forced List, on laisse le natif gerer.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class TameEventHandler {

    private static final Random RNG = new Random();

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        // L'event se declenche UNE FOIS PAR MAIN : sans ce filtre, apprivoiser avec
        // une croquette en main droite re-declenchait immediatement le cas "main vide"
        // avec la main gauche -> toggle Assis parasite + double son.
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        // Le Soigneur est un PNJ : jamais apprivoisable
        if (target instanceof com.lex3d.ultimatezootaming.entities.ZooKeeperEntity) return;
        // Seuls les ANIMAUX sont apprivoisables : jamais les PNJ ni les monstres
        if (target instanceof net.minecraft.world.entity.npc.AbstractVillager
                || target instanceof net.minecraft.world.entity.monster.Enemy
                || target.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) return;

        Player player = event.getEntity();
        ItemStack stack = player.getItemInHand(event.getHand());

        // --- CAS 1 : main vide sur un familier -> toggle Assis/Debout ---
        if (stack.isEmpty() && !player.isShiftKeyDown()) {
            boolean handled = target.getCapability(CapabilityHandler.TAMING_DATA).map(data -> {
                if (!data.isTamed() || !player.getUUID().equals(data.getOwnerUUID())) {
                    return false;
                }
                // Un animal assigne a un enclos n'est PAS assis au clic (sinon il en
                // sortirait) : on laisse le clic tranquille pour ne pas le retirer.
                if (data.isInZoneMode()) {
                    return false;
                }
                data.setSitting(!data.isSitting());
                String key = data.isSitting()
                        ? "message.ultimatezootaming.now_sitting"
                        : "message.ultimatezootaming.now_standing";
                player.displayClientMessage(
                        Component.translatable(key, target.getDisplayName()), true);
                event.getLevel().playSound(null, target.blockPosition(),
                        SoundEvents.ARMOR_EQUIP_LEATHER,
                        SoundSource.NEUTRAL, 0.6f, data.isSitting() ? 0.8f : 1.2f);
                return true;
            }).orElse(false);

            if (handled) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }

        if (!(stack.getItem() instanceof KibbleItem kibble)) return;

        // --- CAS 2 : croquette sur un mob DEJA TAME -> reproduction (in love) ---
        Boolean lovedHandled = target.getCapability(CapabilityHandler.TAMING_DATA).map(data -> {
            if (!data.isTamed()) return false;
            return tryBreeding(event, player, target, kibble, stack);
        }).orElse(false);
        if (lovedHandled) return;

        // --- CAS 3 : croquette sur un mob SAUVAGE -> taming Croquettes+RNG ---
        if (target instanceof TamableAnimal && !isModForced(target)) {
            return; // le systeme natif du mob gere deja le clic-droit
        }

        target.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            if (data.isTamed()) return;
            // Deja apprivoise ? croquette = reproduction (adulte) ou croissance (bebe)
            if (data.isTamed() && player.getUUID().equals(data.getOwnerUUID())
                    && target instanceof net.minecraft.world.entity.animal.Animal animal
                    && kibble.getDiet().matches(target)) {
                if (animal.isBaby()) {
                    animal.ageUp((int) (animal.getAge() / -20), true);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                } else if (animal.canFallInLove()) {
                    animal.setInLove(player);
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
            attemptTaming(event, player, target, kibble, stack, data);
        });

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * Reproduction : une croquette du BON regime sur un Animal adulte tame le met
     * "in love" (coeurs + accouplement possible), exactement comme sa nourriture
     * vanilla. Deux mobs in love proches = bebe (le flux Maternite prend le relais
     * via BirthEventHandler). Renvoie true si l'event a ete consomme.
     */
    private static boolean tryBreeding(PlayerInteractEvent.EntityInteract event, Player player,
                                       LivingEntity target, KibbleItem kibble, ItemStack stack) {
        if (!(target instanceof Animal animal)) return false;

        // Regime incorrect -> message discret, on consomme quand meme le clic
        // (pour eviter que le clic tombe dans le systeme natif du mob par accident)
        if (!kibble.getDiet().matches(target)) {
            player.displayClientMessage(
                    Component.translatable("message.ultimatezootaming.wrong_diet"), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }

        if (animal.isBaby()) {
            // Croquette sur un bebe tame : accelere sa croissance (comme la nourriture vanilla)
            animal.ageUp((int) (-animal.getAge() / 20 * 0.1f), true);
        } else if (animal.getAge() == 0 && animal.canFallInLove()) {
            animal.setInLove(player);
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.HEART,
                        animal.getX(), animal.getY() + animal.getBbHeight() * 0.8, animal.getZ(),
                        6, 0.3, 0.3, 0.3, 0.02);
            }
        } else {
            // en cooldown de reproduction : rien a faire, mais on informe
            player.displayClientMessage(
                    Component.translatable("message.ultimatezootaming.breeding_cooldown"), true);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return true;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    private static void attemptTaming(PlayerInteractEvent.EntityInteract event, Player player,
                                      LivingEntity target, KibbleItem kibble, ItemStack stack, TamingData data) {
        float chance = kibble.getTier().getBaseChance();
        boolean dietMatches = kibble.getDiet().matches(target);
        if (!dietMatches) chance *= 0.5f;
        // Multiplicateur d'equilibrage global (config serveur)
        chance *= (float) (double) com.lex3d.ultimatezootaming.config.ZooServerConfig.GLOBAL_CAPTURE_MULTIPLIER.get();

        data.addTrust(kibble.getTier().getTrustGain());

        boolean success = RNG.nextFloat() < (chance * (data.getTrust() / 100f + 0.15f))
                && data.getTrust() >= kibble.getTier().getTrustRequired();

        double px = target.getX();
        double py = target.getY() + target.getBbHeight() / 2.0;
        double pz = target.getZ();

        if (success) {
            data.setOwnerUUID(player.getUUID());
            data.setForcedTame(true);
            if (target instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            ZooSavedData.get((ServerLevel) event.getLevel()).addFamiliar(player.getUUID(), target.getUUID());
            // TENDANCES : si cette espece etait reclamee par les visiteurs, on satisfait
            // la demande et on verse une PRIME au directeur.
            ServerLevel sl = (ServerLevel) event.getLevel();
            var sid = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            if (sid != null) {
                int prime = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl)
                        .fulfillDemand(sid.toString());
                if (prime > 0) {
                    var vault = com.lex3d.ultimatezootaming.blocks.ZooVaultBlock.anyVault(sl);
                    if (vault != null) vault.deposit(prime);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u2605 ")
                            .withStyle(net.minecraft.ChatFormatting.GOLD)
                            .append(net.minecraft.network.chat.Component.translatable(
                                    "message.ultimatezootaming.demand_fulfilled",
                                    net.minecraft.network.chat.Component.translatable(target.getType().getDescriptionId()),
                                    prime).withStyle(net.minecraft.ChatFormatting.GOLD)), false);
                }
            }
            sl.sendParticles(ParticleTypes.HEART, px, py, pz, 8, 0.3, 0.3, 0.3, 0.02);
            event.getLevel().playSound(null, target.blockPosition(), ModSounds.TAME_SUCCESS.get(),
                    SoundSource.NEUTRAL, 1.0f, 1.0f);
        } else {
            ((ServerLevel) event.getLevel()).sendParticles(ParticleTypes.SMOKE, px, py, pz, 6, 0.3, 0.3, 0.3, 0.02);
            event.getLevel().playSound(null, target.blockPosition(), ModSounds.TAME_FAIL.get(),
                    SoundSource.NEUTRAL, 0.7f, 1.0f);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    /** Devine le modId de l'entite depuis son registry name et verifie la Forced List. */
    private static boolean isModForced(LivingEntity entity) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return id != null && ConfigSyncManager.isModForced(id.getNamespace());
    }
}
