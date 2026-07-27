package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.items.VisitorGearItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Resout geo, texture et animation depuis le nom de modele porte par l'item. */
public class VisitorGearModel extends GeoModel<VisitorGearItem> {

    @Override
    public ResourceLocation getModelResource(VisitorGearItem item) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "geo/gear/" + item.getModelName() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VisitorGearItem item) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "textures/item/gear/" + item.getTextureName() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(VisitorGearItem item) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "animations/gear/" + item.getModelName() + ".animation.json");
    }
}
