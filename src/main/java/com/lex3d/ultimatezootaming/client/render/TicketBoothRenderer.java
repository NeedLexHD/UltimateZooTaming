package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.TicketBoothBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Renderer GeckoLib du guichet de billetterie 1x2.
 * geo     : geo/block/ticket_booth.geo.json
 * texture : textures/block/ticket_booth.png
 * anim    : animations/block/ticket_booth.animation.json
 *
 * A enregistrer dans ClientSetup :
 *   BlockEntityRenderers.register(ModBlockEntities.TICKET_BOOTH.get(),
 *       ctx -> new TicketBoothRenderer());
 */
public class TicketBoothRenderer extends GeoBlockRenderer<TicketBoothBlockEntity> {

    public TicketBoothRenderer() {
        super(new Model());
    }

    private static class Model extends GeoModel<TicketBoothBlockEntity> {

        private static final ResourceLocation MODEL   = new ResourceLocation(
                "ultimatezootaming", "geo/block/ticket_booth.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation(
                "ultimatezootaming", "textures/block/ticket_booth.png");
        private static final ResourceLocation ANIM    = new ResourceLocation(
                "ultimatezootaming", "animations/block/ticket_booth.animation.json");

        @Override public ResourceLocation getModelResource(TicketBoothBlockEntity e)   { return MODEL;   }
        @Override public ResourceLocation getTextureResource(TicketBoothBlockEntity e) { return TEXTURE; }
        @Override public ResourceLocation getAnimationResource(TicketBoothBlockEntity e){ return ANIM;    }
    }
}
