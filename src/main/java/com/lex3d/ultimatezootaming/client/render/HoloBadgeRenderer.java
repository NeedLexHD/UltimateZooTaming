package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.config.ZooClientConfig;
import com.lex3d.ultimatezootaming.core.init.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "Affiche le Holo-Badge (render client-side) sur tes familiers pour les distinguer
 *  du betail sauvage." + "L'Holo-badge est calcule 100% sur le GPU du client."
 *
 * Ne s'affiche que si : le joueur tient le Sifflet en main ET l'option showHoloBadge
 * est activee dans la config ET l'entite est un familier tame (peu importe le
 * proprietaire, pour que tu puisses reperer aussi les familiers d'autres joueurs
 * en multi si besoin -- facile a restreindre a "MES familiers" si tu preferes).
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID, value = Dist.CLIENT)
public class HoloBadgeRenderer {

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        if (!ZooClientConfig.SHOW_HOLO_BADGE.get()) return;
        LivingEntity living = event.getEntity();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        boolean holdingWhistle = player.getMainHandItem().is(ModItems.WHISTLE.get())
                || player.getOffhandItem().is(ModItems.WHISTLE.get());
        if (!holdingWhistle) return;

        boolean isTamed = living.getCapability(CapabilityHandler.TAMING_DATA)
                .map(com.lex3d.ultimatezootaming.capability.TamingData::isTamed)
                .orElse(false);
        if (!isTamed) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();

        poseStack.pushPose();
        poseStack.translate(0, living.getBbHeight() + 0.5, 0);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025f, -0.025f, 0.025f);

        var font = Minecraft.getInstance().font;
        Component label = Component.literal("\u2764"); // coeur, simple et lisible, pas besoin de texture custom
        float x = -font.width(label) / 2f;

        RenderSystem.enableBlend();
        font.drawInBatch(label, x, 0, 0x55FF55, false, poseStack.last().pose(), buffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, event.getPackedLight());

        poseStack.popPose();
    }
}
