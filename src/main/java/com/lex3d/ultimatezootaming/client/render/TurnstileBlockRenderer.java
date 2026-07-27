package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.ZooEntranceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Renderer GeckoLib du tourniquet d'entree du zoo.
 * geo     : geo/block/zoo_entrance_turnstile.geo.json
 * texture : textures/block/zoo_entrance_turnstile.png
 * anim    : animations/block/zoo_entrance_turnstile.animation.json
 *
 * A enregistrer dans ClientSetup :
 *   BlockEntityRenderers.register(ModBlockEntities.ZOO_ENTRANCE.get(),
 *       ctx -> new TurnstileBlockRenderer());
 */
public class TurnstileBlockRenderer extends GeoBlockRenderer<ZooEntranceBlockEntity> {

    public TurnstileBlockRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<ZooEntranceBlockEntity> {

        private static final ResourceLocation MODEL   = new ResourceLocation(
                "ultimatezootaming", "geo/block/zoo_entrance_turnstile.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                "ultimatezootaming", "textures/block/zoo_entrance_turnstile.png");
        private static final ResourceLocation ANIM    = new ResourceLocation(
                "ultimatezootaming", "animations/block/zoo_entrance_turnstile.animation.json");

        @Override public ResourceLocation getModelResource(ZooEntranceBlockEntity e)   { return MODEL;   }
        @Override public ResourceLocation getTextureResource(ZooEntranceBlockEntity e) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(ZooEntranceBlockEntity e){ return ANIM;    }
    }
}
