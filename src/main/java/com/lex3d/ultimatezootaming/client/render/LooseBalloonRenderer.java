package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.entities.LooseBalloonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Rendu du ballon echappe : on reutilise simplement l'item ballon de la bonne
 * couleur, rendu en flottant et en rotation lente. Pas de modele dedie, pas de
 * texture supplementaire.
 */
public class LooseBalloonRenderer extends EntityRenderer<LooseBalloonEntity> {

    public LooseBalloonRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(LooseBalloonEntity entity, float yaw, float partialTick,
                       PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        pose.translate(0, 0.4, 0);
        // Rotation lente sur lui-meme + leger tangage
        float spin = (entity.tickCount + partialTick) * 2.0f;
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(spin * 0.05) * 8f));
        pose.scale(1.6f, 1.6f, 1.6f);

        ItemStack stack = balloonStack(entity.getColor());
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,
                ItemDisplayContext.GROUND, light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                pose, buffer, entity.level(), 0);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffer, light);
    }

    /** L'item ballon correspondant a la couleur transportee par l'entite. */
    private static ItemStack balloonStack(int color) {
        return new ItemStack(switch (Math.floorMod(color, 6)) {
            case 1 -> com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_BLUE.get();
            case 2 -> com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_GREEN.get();
            case 3 -> com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_YELLOW.get();
            case 4 -> com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_PINK.get();
            case 5 -> com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_PURPLE.get();
            default -> com.lex3d.ultimatezootaming.core.init.ModItems.BALLOON_RED.get();
        });
    }

    @Override
    public ResourceLocation getTextureLocation(LooseBalloonEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
