package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.items.ShopProductItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modele des produits de boutique ayant un rendu 3D (casquette, badge). */
public class ShopProductModel extends GeoModel<ShopProductItem> {
    @Override
    public ResourceLocation getModelResource(ShopProductItem item) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "geo/gear/" + item.getModelName() + ".geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(ShopProductItem item) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "textures/item/gear/" + item.getModelName() + ".png");
    }
    @Override
    public ResourceLocation getAnimationResource(ShopProductItem item) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "animations/gear/" + item.getModelName() + ".animation.json");
    }
}
