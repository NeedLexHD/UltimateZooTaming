package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.ZooEntranceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modele GeckoLib de la Entree du zoo (tourniquet au passage). */
public class ZooEntranceModel extends GeoModel<ZooEntranceBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ZooEntranceBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/zoo_entrance.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZooEntranceBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "textures/block/zoo_entrance_geo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZooEntranceBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/zoo_entrance.animation.json");
    }
}
