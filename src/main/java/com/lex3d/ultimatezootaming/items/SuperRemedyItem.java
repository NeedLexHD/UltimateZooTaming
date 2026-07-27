package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.network.chat.Component;
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
 * Remede superieur : la SEULE cure des maladies graves — et c'est au directeur
 * (le joueur) de l'administrer, le veterinaire n'y touche pas.
 */
public class SuperRemedyItem extends Item {

    public SuperRemedyItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        var opt = target.getCapability(CapabilityHandler.TAMING_DATA).resolve();
        if (opt.isEmpty()) return InteractionResult.PASS;
        var data = opt.get();
        if (!data.isTamed() || !data.isSick()) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.super_remedy_healthy"), true);
            return InteractionResult.CONSUME;
        }
        data.setSick(false);
        data.setSevereSick(false);
        data.setMiseryTimer(0);
        data.setSatisfaction(Math.max(data.getSatisfaction(), 60));
        stack.shrink(1);
        if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(),
                    6, 0.4, 0.3, 0.4, 0.0);
            level.playSound(null, target.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.4f);
        }
        player.displayClientMessage(Component.translatable(
                "message.ultimatezootaming.super_remedy_used", target.getName()), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.super_remedy"));
    }
}
