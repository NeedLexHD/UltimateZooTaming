package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.DecorBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Modele partage des decos GeckoLib : panneau / banc / poubelle. */
public class DecorModel extends GeoModel<DecorBlockEntity> {

    @Override
    public ResourceLocation getModelResource(DecorBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/" + be.decorKey() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(DecorBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "textures/block/" + be.decorKey() + "_geo.png");
    }

    @Override
    public ResourceLocation getAnimationResource(DecorBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID,
                "animations/" + be.decorKey() + ".animation.json");
    }
}
