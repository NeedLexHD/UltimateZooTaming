package com.lex3d.ultimatezootaming.client;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dessine une icone flottante au-dessus des familiers :
 *   - une pastille coloree = son trait (couleur signature par trait)
 *   - une croix maladie bien visible s'il est malade (prioritaire)
 * Les donnees viennent du ClientBadgeCache (pousse par le serveur).
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FamiliarBadgeRenderer {

    // Couleur signature par index de trait (voir TamingData.Trait ordinal)
    // 0=NONE 1=GLUTTON 2=CUDDLY 3=GRUMPY 4=ENERGETIC 5=HARDY 6=SOCIAL
    private static final int[] TRAIT_COLORS = {
            0x000000,          // NONE (non affiche)
            0xFFD24B,          // GLUTTON - jaune/or
            0xFF7BC6,          // CUDDLY - rose
            0xC0603A,          // GRUMPY - brun-rouge
            0x4BD2FF,          // ENERGETIC - cyan
            0x8B8B8B,          // HARDY - gris acier
            0x8BE04B           // SOCIAL - vert
    };

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        int id = entity.getId();
        if (!ClientBadgeCache.hasData(id)) return;

        boolean sick = ClientBadgeCache.isSick(id);
        int trait = ClientBadgeCache.traitOf(id);
        if (!sick && trait <= 0) return;

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();

        pose.pushPose();
        // Se placer au-dessus de la tete de l'entite
        pose.translate(0, entity.getBbHeight() + 0.6, 0);
        // Billboard : toujours face a la camera
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.025f, -0.025f, 0.025f);

        var matrix = pose.last().pose();
        var consumer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.debugQuads());

        if (sick) {
            // Croix verte de maladie (bien visible)
            drawCross(matrix, consumer, 0x60E080);
        } else {
            // Pastille coloree du trait
            int color = TRAIT_COLORS[Math.min(trait, TRAIT_COLORS.length - 1)];
            drawDot(matrix, consumer, color);
        }

        pose.popPose();
    }

    private static void drawDot(org.joml.Matrix4f m, com.mojang.blaze3d.vertex.VertexConsumer c, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float s = 3.0f;
        // fond sombre (contour)
        quad(m, c, -s - 1, -s - 1, s + 1, s + 1, 0f, 0f, 0f, 1f);
        // pastille coloree
        quad(m, c, -s, -s, s, s, r, g, b, 1f);
    }

    private static void drawCross(org.joml.Matrix4f m, com.mojang.blaze3d.vertex.VertexConsumer c, int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        // barre verticale + horizontale (une croix medicale)
        quad(m, c, -1.2f, -4f, 1.2f, 4f, r, g, b, 1f);
        quad(m, c, -4f, -1.2f, 4f, 1.2f, r, g, b, 1f);
    }

    private static void quad(org.joml.Matrix4f m, com.mojang.blaze3d.vertex.VertexConsumer c,
                             float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
        c.vertex(m, x0, y1, 0).color(r, g, b, a).endVertex();
        c.vertex(m, x1, y1, 0).color(r, g, b, a).endVertex();
        c.vertex(m, x1, y0, 0).color(r, g, b, a).endVertex();
        c.vertex(m, x0, y0, 0).color(r, g, b, a).endVertex();
    }
}
