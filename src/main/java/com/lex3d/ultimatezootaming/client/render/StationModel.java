package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.StationBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modele GeckoLib des bornes : photo_spot / feed_station / water_jet. */
public class StationModel extends GeoModel<StationBlockEntity> {

    @Override
    public ResourceLocation getModelResource(StationBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/" + be.stationKey() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StationBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "textures/block/" + be.stationKey() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(StationBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "animations/" + be.stationKey() + ".animation.json");
    }
}
