package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modele GeckoLib de la Caisse du Zoo (coffre-fort, molette au depot). */
public class ZooVaultModel extends GeoModel<ZooVaultBlockEntity> {

    @Override
    public ResourceLocation getModelResource(ZooVaultBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/zoo_vault.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZooVaultBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "textures/block/zoo_vault_geo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZooVaultBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/zoo_vault.animation.json");
    }
}
