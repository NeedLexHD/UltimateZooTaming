package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.TrappingCageBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class TrappingCageModel extends GeoModel<TrappingCageBlockEntity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(UltimateZooTame.MODID, "geo/trapping_cage.geo.json");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(UltimateZooTame.MODID, "animations/trapping_cage.animation.json");

    @Override
    public ResourceLocation getModelResource(TrappingCageBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(TrappingCageBlockEntity animatable) {
        // Une identite visuelle par taille : metal / bronze / acier sombre / obsidienne
        return switch (animatable.getSize()) {
            case SMALL -> new ResourceLocation(UltimateZooTame.MODID, "textures/block/trapping_cage_geo_small.png");
            case MEDIUM -> new ResourceLocation(UltimateZooTame.MODID, "textures/block/trapping_cage_geo_medium.png");
            case LARGE -> new ResourceLocation(UltimateZooTame.MODID, "textures/block/trapping_cage_geo_large.png");
            case UNBREAKABLE -> new ResourceLocation(UltimateZooTame.MODID, "textures/block/trapping_cage_geo_unbreakable.png");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(TrappingCageBlockEntity animatable) {
        return ANIMATIONS;
    }
}
