package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.IncubatorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class IncubatorModel extends GeoModel<IncubatorBlockEntity> {
    @Override
    public ResourceLocation getModelResource(IncubatorBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/incubator.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(IncubatorBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "textures/block/incubator_3d.png");
    }
    @Override
    public ResourceLocation getAnimationResource(IncubatorBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/incubator.animation.json");
    }
}
