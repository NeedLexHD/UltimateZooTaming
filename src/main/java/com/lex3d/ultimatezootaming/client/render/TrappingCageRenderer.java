package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.TrappingCageBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Wrapper autour du GeoBlockRenderer de GeckoLib qui ajoute deux comportements :
 *
 * 1. MULTIBLOCK : seul le MASTER rend le modele, scale N fois (une cage 3x3 =
 *    un seul grand modele couvrant le footprint 3x3, pas 9 petites cages).
 *    Le scale est applique au PoseStack AVANT le rendu Geo : le renderer interne
 *    translate ensuite de (0.5, 0, 0.5) qui, scale, devient le centre du footprint.
 *
 * 2. APPAT REEL : l'ItemStack pose est rendu en 3D au-dessus du socle central,
 *    en rotation lente (donnee synchronisee via getUpdateTag/getUpdatePacket du BE).
 */
public class TrappingCageRenderer implements BlockEntityRenderer<TrappingCageBlockEntity> {

    /** Le vrai renderer GeckoLib, delegue (jamais sous-classe pour override render :
     *  les signatures internes de GeoBlockRenderer varient entre versions 4.x). */
    private final software.bernie.geckolib.renderer.GeoBlockRenderer<TrappingCageBlockEntity> geoRenderer =
            new software.bernie.geckolib.renderer.GeoBlockRenderer<>(new TrappingCageModel());

    public TrappingCageRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context) {
        // le contexte n'est pas necessaire mais la factory d'enregistrement le fournit
    }

    @Override
    public void render(TrappingCageBlockEntity cage, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Seul le master rend : les cellules "part" du multiblock sont invisibles
        // (leur presence physique sert a la detection par collision, pas au visuel).
        if (!cage.isMaster()) return;

        int n = cage.getSize().getRadius();

        poseStack.pushPose();
        poseStack.scale(n, n, n);
        geoRenderer.render(cage, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        renderBaitItem(cage, partialTick, poseStack, bufferSource, packedLight, packedOverlay, n);
    }

    /** L'appat pose, visible en 3D au-dessus du socle central, en rotation lente. */
    private void renderBaitItem(TrappingCageBlockEntity cage, float partialTick, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight, int packedOverlay, int n) {
        ItemStack bait = cage.getBait();
        if (bait.isEmpty() || cage.getLevel() == null) return;

        poseStack.pushPose();
        // Centre du footprint N x N, juste au-dessus du socle (socle a y=3/16 * N)
        poseStack.translate(n / 2.0, 0.35 * n, n / 2.0);
        poseStack.scale(0.5f * n, 0.5f * n, 0.5f * n);

        float angle = (cage.getLevel().getGameTime() + partialTick) * 2.0f;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                bait, ItemDisplayContext.GROUND, packedLight, packedOverlay,
                poseStack, bufferSource, cage.getLevel(), 0);

        poseStack.popPose();
    }
}
