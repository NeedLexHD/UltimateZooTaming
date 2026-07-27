package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.blocks.IncubatorBlockEntity;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.items.GeneticSampleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/**
 * ECLOSION : fait naitre le bebe a la fin d'un cycle d'incubation, en croisant
 * les deux echantillons genetiques inseres.
 *
 * L'heritage suit exactement la meme table que la reproduction naturelle :
 * 60% le trait d'un "parent", 30% un trait au hasard, 10% une mutation qui
 * augmente la rarete d'un cran.
 */
public final class IncubatorHandler {

    private static final Random RNG = new Random();

    private IncubatorHandler() {}

    /** Fait naitre l'animal issu des deux echantillons de la machine. */
    public static void hatch(ServerLevel level, BlockPos pos, IncubatorBlockEntity be) {
        ItemStack a = be.getSampleA();
        ItemStack b = be.getSampleB();
        if (!GeneticSampleItem.isValid(a) || !GeneticSampleItem.isValid(b)) return;

        // L'espece est garantie identique par le bloc a l'insertion
        EntityType<?> type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getValue(ResourceLocation.tryParse(GeneticSampleItem.speciesOf(a)));
        if (type == null) return;
        if (!(type.create(level) instanceof Animal baby)) return;

        baby.setBaby(true);
        baby.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                RNG.nextFloat() * 360f, 0f);
        level.addFreshEntity(baby);

        TamingData d = baby.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (d != null) {
            // Le bebe nait apprivoise, au nom du proprietaire du zoo le plus proche
            var owner = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 64, false);
            if (owner != null) d.setOwnerUUID(owner.getUUID());
            d.setForcedTame(true);
            d.setTrust(100f);
            d.setCaptureDay(com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).getDay());

            // Generation : un cran au-dessus du plus avance des deux donneurs
            d.setGeneration(Math.max(GeneticSampleItem.generationOf(a),
                    GeneticSampleItem.generationOf(b)) + 1);

            // Trait : 60% herite, 30% aleatoire, 10% mutation
            var traits = TamingData.Trait.values();
            float r = RNG.nextFloat();
            boolean mutated = false;
            if (r < 0.60f) {
                String pick = RNG.nextBoolean()
                        ? GeneticSampleItem.traitOf(a) : GeneticSampleItem.traitOf(b);
                d.setTrait(parseTrait(pick));
            } else if (r < 0.90f) {
                d.setTrait(traits[RNG.nextInt(traits.length)]);
            } else {
                d.setTrait(traits[1 + RNG.nextInt(traits.length - 1)]);
                mutated = true;
            }

            // Rarete : la meilleure des deux, +1 en cas de mutation
            int inherited = Math.max(GeneticSampleItem.rarityOf(a), GeneticSampleItem.rarityOf(b));
            d.setRarity(Math.min(3, mutated ? inherited + 1 : inherited));

            // Annonce publique si le resultat sort de l'ordinaire
            if (mutated || d.getRarity() > 0) {
                for (var p : level.getServer().getPlayerList().getPlayers()) {
                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal("\u2726 ")
                            .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)
                            .append(net.minecraft.network.chat.Component.translatable(
                                    "message.ultimatezootaming.incubator_hatched",
                                    net.minecraft.network.chat.Component
                                            .translatable(type.getDescriptionId()),
                                    net.minecraft.network.chat.Component.translatable(
                                            "gui.ultimatezootaming.rarity." + Math.max(1, d.getRarity())))
                                    .withStyle(net.minecraft.ChatFormatting.WHITE)));
                }
            }
        }

        // Effets d'eclosion
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.4, 0.3, 0.4, 0.03);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 20, 0.5, 0.4, 0.5, 0.05);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHICKEN_EGG,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.2f);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.6f);
    }

    /** Convertit le nom de trait stocke dans l'echantillon, avec repli sur NONE. */
    private static TamingData.Trait parseTrait(String name) {
        try {
            return TamingData.Trait.valueOf(name);
        } catch (IllegalArgumentException e) {
            return TamingData.Trait.NONE;
        }
    }
}
