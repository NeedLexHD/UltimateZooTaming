package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.entities.KeeperTask;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Affiche AU-DESSUS DE LA TETE ce que l'employe est en train de faire.
 *
 * Visible seulement de pres (8 blocs) pour ne pas encombrer la vue d'ensemble
 * du parc, et masque quand il ne fait rien de notable.
 */
public class KeeperTaskLayer extends GeoRenderLayer<ZooKeeperEntity> {

    /** Distance maximale d'affichage, au carre. */
    private static final double VIEW_DIST = 8.0 * 8.0;

    public KeeperTaskLayer(GeoRenderer<ZooKeeperEntity> renderer) {
        super(renderer);
    }

    public void render(PoseStack pose, ZooKeeperEntity keeper, BakedGeoModel model,
                       net.minecraft.client.renderer.RenderType renderType,
                       MultiBufferSource buffer,
                       net.minecraft.client.renderer.texture.OverlayTexture overlay,
                       float partialTick, int packedLight, int packedOverlay) {

        if (!com.lex3d.ultimatezootaming.config.ZooClientConfig.SHOW_KEEPER_TASK.get()) return;

        KeeperTask task = keeper.getTask();
        if (task == KeeperTask.IDLE) return;
        if (keeper.isSleeping()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.distanceToSqr(keeper) > VIEW_DIST) return;

        Font font = mc.font;
        Component label = Component.literal(task.icon + " ")
                .append(Component.translatable(task.translationKey()));
        float width = font.width(label) / 2f;

        pose.pushPose();
        pose.translate(0, keeper.getBbHeight() + 0.55, 0);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(-0.022f, -0.022f, 0.022f);

        int bg = (int) (mc.options.getBackgroundOpacity(0.25f) * 255f) << 24;
        font.drawInBatch(label, -width, 0, 0xFFFFFF, false, pose.last().pose(),
                buffer, Font.DisplayMode.NORMAL, bg, packedLight);
        pose.popPose();
    }
}
