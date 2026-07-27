package com.lex3d.ultimatezootaming.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Carte du Zoo : la vue du directeur — territoire, chemins, tout le parc en direct. */
public class ZooMapItem extends Item {

    public ZooMapItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            com.lex3d.ultimatezootaming.core.network.RequestMapC2SPacket.sendMapTo(
                    sp, player.blockPosition().getX(), player.blockPosition().getZ());
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
