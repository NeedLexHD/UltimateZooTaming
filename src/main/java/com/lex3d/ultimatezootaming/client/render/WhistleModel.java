package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.items.WhistleItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class WhistleModel extends GeoModel<WhistleItem> {
    @Override
    public ResourceLocation getModelResource(WhistleItem item) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/whistle.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(WhistleItem item) {
        return new ResourceLocation(UltimateZooTame.MODID, "textures/item/whistle_3d.png");
    }
    @Override
    public ResourceLocation getAnimationResource(WhistleItem item) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/whistle.animation.json");
    }
}
