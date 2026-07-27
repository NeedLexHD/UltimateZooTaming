package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.NetTrapBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class NetTrapModel extends GeoModel<NetTrapBlockEntity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(UltimateZooTame.MODID, "geo/net_trap.geo.json");
    private static final ResourceLocation ANIMATIONS =
            new ResourceLocation(UltimateZooTame.MODID, "animations/net_trap.animation.json");

    @Override
    public ResourceLocation getModelResource(NetTrapBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(NetTrapBlockEntity animatable) {
        // Une texture par tier (corde beige / metal / abysse bleu)
        return switch (animatable.getTier()) {
            case SMALL -> new ResourceLocation(UltimateZooTame.MODID, "textures/block/net_trap_geo_small.png");
            case REINFORCED -> new ResourceLocation(UltimateZooTame.MODID, "textures/block/net_trap_geo_reinforced.png");
            case POOL -> new ResourceLocation(UltimateZooTame.MODID, "textures/block/net_trap_geo_pool.png");
        };
    }

    @Override
    public ResourceLocation getAnimationResource(NetTrapBlockEntity animatable) {
        return ANIMATIONS;
    }
}
