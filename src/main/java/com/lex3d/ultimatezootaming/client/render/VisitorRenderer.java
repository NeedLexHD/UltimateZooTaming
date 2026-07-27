package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VisitorRenderer extends GeoEntityRenderer<VisitorEntity> {

    public VisitorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VisitorModel());
        this.shadowRadius = 0.4f;
        // Affiche l'objet tenu (item vanilla) dans la main du visiteur
        addRenderLayer(new VisitorItemLayer(this));
        addRenderLayer(new VisitorHeadLayer(this)); // casquette du zoo
    }

    /** Les ENFANTS sont rendus plus petits (65% de la taille adulte). */
    @Override
    public void scaleModelForRender(float widthScale, float heightScale, PoseStack poseStack,
                                    VisitorEntity animatable, BakedGeoModel model,
                                    boolean isReRender, float partialTick, int packedLight,
                                    int packedOverlay) {
        if (animatable.isChild()) {
            widthScale *= 0.65f;
            heightScale *= 0.65f;
            this.shadowRadius = 0.28f;
        } else {
            this.shadowRadius = 0.4f;
        }
        super.scaleModelForRender(widthScale, heightScale, poseStack, animatable, model,
                isReRender, partialTick, packedLight, packedOverlay);
    }
}
