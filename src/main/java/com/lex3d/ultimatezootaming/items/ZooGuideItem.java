package com.lex3d.ultimatezootaming.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Guide du Zoo : clic droit = ouvre le manuel complet du mod. */
public class ZooGuideItem extends Item {

    public ZooGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                    net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                            com.lex3d.ultimatezootaming.client.ClientSetup.openGuideScreen());
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
