package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.ShopBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modele GeckoLib de la caisse enregistreuse (tiroir anime a la vente). */
public class CashRegisterModel extends GeoModel<ShopBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ShopBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/cash_register.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ShopBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "textures/block/cash_register_geo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ShopBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/cash_register.animation.json");
    }
}
