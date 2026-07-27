package com.lex3d.ultimatezootaming.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Fourrage : nourriture de mangeoire pour le QUOTIDIEN. Les animaux le mangent
 * pour leur bonheur (score Nourriture), mais il ne declenche NI reproduction NI
 * croissance acceleree — pour ca, mets des croquettes dans la mangeoire.
 */
public class FodderItem extends Item {

    private final KibbleItem.Diet diet;

    public FodderItem(KibbleItem.Diet diet, Properties properties) {
        super(properties);
        this.diet = diet;
    }

    public KibbleItem.Diet getDiet() {
        return diet;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.fodder.1"));
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.fodder.2"));
    }
}
