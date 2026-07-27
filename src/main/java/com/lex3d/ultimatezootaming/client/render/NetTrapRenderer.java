package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.NetTrapBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Meme pattern que TrappingCageRenderer : delegation au GeoBlockRenderer (jamais
 * de sous-classe pour eviter les signatures internes qui varient), rendu du
 * MASTER uniquement, scale N pour couvrir le footprint, appat reel en 3D.
 */
public class NetTrapRenderer implements BlockEntityRenderer<NetTrapBlockEntity> {

    private final software.bernie.geckolib.renderer.GeoBlockRenderer<NetTrapBlockEntity> geoRenderer =
            new software.bernie.geckolib.renderer.GeoBlockRenderer<>(new NetTrapModel());

    public NetTrapRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(NetTrapBlockEntity net, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!net.isMaster()) return;

        int n = net.getTier().getRadius();

        poseStack.pushPose();
        poseStack.scale(n, n, n);
        geoRenderer.render(net, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        ItemStack bait = net.getBait();
        if (bait.isEmpty() || net.getLevel() == null) return;

        poseStack.pushPose();
        poseStack.translate(n / 2.0, 0.28 * n, n / 2.0);
        poseStack.scale(0.4f * n, 0.4f * n, 0.4f * n);
        float angle = (net.getLevel().getGameTime() + partialTick) * 2.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        Minecraft.getInstance().getItemRenderer().renderStatic(
                bait, ItemDisplayContext.GROUND, packedLight, packedOverlay,
                poseStack, bufferSource, net.getLevel(), 0);
        poseStack.popPose();
    }
}
