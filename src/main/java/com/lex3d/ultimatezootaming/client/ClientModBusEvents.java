package com.lex3d.ultimatezootaming.client;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.client.render.NetTrapRenderer;
import com.lex3d.ultimatezootaming.client.render.ZooKeeperRenderer;
import com.lex3d.ultimatezootaming.core.init.ModEntities;
import com.lex3d.ultimatezootaming.client.render.TrappingCageRenderer;
import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientModBusEvents {


    public static final KeyMapping OPEN_MATERNITY = new KeyMapping(
            "key.ultimatezootaming.open_maternity",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            "key.categories.ultimatezootaming"
    );

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(
                com.lex3d.ultimatezootaming.core.init.ModMenuTypes.SHOP.get(),
                com.lex3d.ultimatezootaming.client.gui.ShopScreen::new));
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MATERNITY);
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "welfare_bar",
                com.lex3d.ultimatezootaming.client.WelfareOverlay.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TRAPPING_CAGE.get(), TrappingCageRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SHOP.get(),
                com.lex3d.ultimatezootaming.client.render.CashRegisterRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ZOO_VAULT.get(),
                com.lex3d.ultimatezootaming.client.render.ZooVaultRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ZOO_ENTRANCE.get(),
                com.lex3d.ultimatezootaming.client.render.ZooEntranceRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DECOR.get(),
                com.lex3d.ultimatezootaming.client.render.DecorRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STATION.get(),
                com.lex3d.ultimatezootaming.client.render.StationRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FEEDER.get(),
                com.lex3d.ultimatezootaming.client.render.FeederRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.KEEPER_LOCKER.get(),
                com.lex3d.ultimatezootaming.client.render.KeeperLockerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.NET_TRAP.get(), NetTrapRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INCUBATOR.get(),
                ctx -> new com.lex3d.ultimatezootaming.client.render.IncubatorRenderer());
        event.registerEntityRenderer(ModEntities.ZOO_KEEPER.get(), ZooKeeperRenderer::new);
        event.registerEntityRenderer(ModEntities.LOOSE_BALLOON.get(),
                com.lex3d.ultimatezootaming.client.render.LooseBalloonRenderer::new);
        event.registerEntityRenderer(ModEntities.VISITOR.get(),
                com.lex3d.ultimatezootaming.client.render.VisitorRenderer::new);
    }
}
