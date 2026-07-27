package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Remede animal : clic droit sur un familier MALADE le guerit (remet la maladie
 * a zero et redonne un peu de satisfaction). Consomme 1 unite.
 */
public class AnimalRemedyItem extends Item {

    public AnimalRemedyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;

        return target.getCapability(CapabilityHandler.TAMING_DATA).resolve().map(data -> {
            if (data.isSevereSick()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.remedy_too_weak"), true);
                return InteractionResult.CONSUME;
            }
            if (!data.isTamed() || !data.isSick()) {
                return InteractionResult.PASS;
            }
            data.setSick(false);
            data.setMiseryTimer(0);
            data.setSatisfaction(Math.max(data.getSatisfaction(), 50));
            data.addHealCount(); // fiche approfondie : historique de soins

            if (player.level() instanceof ServerLevel level) {
                com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).addMissionProgress(
                        com.lex3d.ultimatezootaming.progression.DailyMission.HEAL_ANIMALS, 1);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, target.blockPosition(), SoundEvents.HONEY_BLOCK_SLIDE,
                        SoundSource.NEUTRAL, 1.0f, 1.4f);
            }
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.remedy_used", target.getDisplayName()), true);

            if (!player.getAbilities().instabuild) stack.shrink(1);
            return InteractionResult.CONSUME;
        }).orElse(InteractionResult.PASS);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.animal_remedy"));
    }
}
