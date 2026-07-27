package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.FeederBlockEntity;
import com.lex3d.ultimatezootaming.core.init.ModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.model.GeoModel;

/** Modele GeckoLib de la mangeoire : texture selon la variante posee. */
public class FeederModel extends GeoModel<FeederBlockEntity> {

    @Override
    public ResourceLocation getModelResource(FeederBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/feeder.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(FeederBlockEntity be) {
        Block block = be.getBlockState().getBlock();
        String variant = "wood";
        if (block == ModBlocks.FEEDER_STONE.get()) variant = "stone";
        else if (block == ModBlocks.FEEDER_ICE.get()) variant = "ice";
        else if (block == ModBlocks.FEEDER_SANDSTONE.get()) variant = "sandstone";
        else if (block == ModBlocks.FEEDER_JUNGLE.get()) variant = "jungle";
        else if (block == ModBlocks.FEEDER_NETHER.get()) variant = "nether";
        return new ResourceLocation(UltimateZooTame.MODID, "textures/block/feeder/feeder_" + variant + ".png");
    }

    @Override
    public void setCustomAnimations(FeederBlockEntity be, long instanceId,
            software.bernie.geckolib.core.animation.AnimationState<FeederBlockEntity> state) {
        var bs = be.getBlockState();
        int level = bs.hasProperty(com.lex3d.ultimatezootaming.blocks.FeederBlock.LEVEL)
                ? bs.getValue(com.lex3d.ultimatezootaming.blocks.FeederBlock.LEVEL) : 0;
        int type = bs.hasProperty(com.lex3d.ultimatezootaming.blocks.FeederBlock.FOOD_TYPE)
                ? bs.getValue(com.lex3d.ultimatezootaming.blocks.FeederBlock.FOOD_TYPE) : 0;
        String[] names = {"food_plant", "food_meat", "food_fish"};
        for (int i = 0; i < names.length; i++) {
            var bone = getAnimationProcessor().getBone(names[i]);
            if (bone == null) continue;
            boolean show = level > 0 && type == i + 1;
            bone.setHidden(!show);
            bone.setScaleY(level == 1 ? 0.35f : 1.0f);
        }
    }

    @Override
    public ResourceLocation getAnimationResource(FeederBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/feeder.animation.json");
    }
}
