package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * SERINGUE DE PRELEVEMENT : clic droit sur un animal apprivoise pour recuperer
 * un Echantillon genetique portant son patrimoine.
 *
 * L'animal perd un peu de satisfaction (le geste n'est pas agreable) et la
 * seringue s'use. Un animal ne peut etre preleve qu'une fois par minute.
 */
public class SamplingSyringeItem extends Item {

    /** Delai avant de pouvoir preleve a nouveau le meme animal (en ticks). */
    private static final long COOLDOWN = 1200L; // 1 minute

    public SamplingSyringeItem(Properties p) { super(p); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!(target instanceof Animal animal)) return InteractionResult.PASS;

        var data = animal.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (data == null || !data.isTamed()) {
            sp.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.sample_wild").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }

        // Un prelevement par minute et par animal
        long now = sp.serverLevel().getGameTime();
        var persist = animal.getPersistentData();
        long last = persist.getLong("uzt_last_sample");
        if (now - last < COOLDOWN) {
            long left = (COOLDOWN - (now - last)) / 20L;
            sp.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.sample_cooldown", left)
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        persist.putLong("uzt_last_sample", now);

        var id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                .getKey(animal.getType());
        if (id == null) return InteractionResult.PASS;

        ItemStack sample = GeneticSampleItem.create(
                id.toString(),
                animal.getName().getString(),
                data.getTrait() == null ? "NONE" : data.getTrait().name(),
                data.getRarity(),
                data.getGeneration());
        player.getInventory().placeItemBackInInventory(sample);

        // Le geste derange un peu l'animal
        data.setSatisfaction(Math.max(0, data.getSatisfaction() - 4));
        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

        sp.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT,
                animal.getX(), animal.getY() + animal.getBbHeight() * 0.6, animal.getZ(),
                5, 0.2, 0.2, 0.2, 0.02);
        sp.serverLevel().playSound(null, animal.blockPosition(),
                SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 0.6f, 1.4f);
        sp.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.sample_taken", animal.getName())
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.CONSUME;
    }
}
