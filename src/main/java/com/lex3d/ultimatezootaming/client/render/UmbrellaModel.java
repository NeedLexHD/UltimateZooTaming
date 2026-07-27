package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.items.UmbrellaItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class UmbrellaModel extends GeoModel<UmbrellaItem> {
    @Override
    public ResourceLocation getModelResource(UmbrellaItem item) {
        // Le modele est le meme pour les 2 variantes (memes cubes) : seule la
        // texture change (l'ajout des oreilles est dans la texture / uv map).
        return new ResourceLocation(UltimateZooTame.MODID,
                item.hasEars() ? "geo/umbrella_kawaii.geo.json" : "geo/umbrella.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(UmbrellaItem item) {
        return new ResourceLocation(UltimateZooTame.MODID,
                item.hasEars() ? "textures/item/umbrella_kawaii.png" : "textures/item/umbrella_3d.png");
    }
    @Override
    public ResourceLocation getAnimationResource(UmbrellaItem item) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/umbrella.animation.json");
    }
}
