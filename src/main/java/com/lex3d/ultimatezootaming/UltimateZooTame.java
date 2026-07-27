package com.lex3d.ultimatezootaming;

import com.lex3d.ultimatezootaming.client.ClientSetup;
import com.lex3d.ultimatezootaming.config.ZooClientConfig;
import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import com.lex3d.ultimatezootaming.core.init.ModBlocks;
import com.lex3d.ultimatezootaming.core.init.ModCreativeTabs;
import com.lex3d.ultimatezootaming.core.init.ModEntities;
import com.lex3d.ultimatezootaming.core.init.ModItems;
import com.lex3d.ultimatezootaming.core.init.ModSounds;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Point d'entree du mod. C'est ici qu'on enregistre tous les DeferredRegisters
 * et qu'on branche les bus (mod bus vs forge bus).
 *
 * - Le "mod bus" (modEventBus) sert au chargement : registres, setup client/serveur, config.
 * - Le "forge bus" (MinecraftForge.EVENT_BUS) sert au gameplay in-game (voir events/*).
 *   Les classes dans events/ utilisent @Mod.EventBusSubscriber donc elles s'auto-enregistrent,
 *   pas besoin de les appeler ici.
 *
 * IMPORTANT : cette classe est chargee sur CLIENT ET SERVEUR DEDIE. Elle ne doit
 * JAMAIS reference directement une classe client-only (Screen, ConfigModScreen,
 * ConfigScreenHandler...) meme a l'interieur d'un DistExecutor -- le simple fait
 * qu'une methode DE CETTE CLASSE reference un type client dans sa signature/corps
 * fait planter le chargement sur serveur dedie, DistExecutor ou pas. Tout code
 * client va dans le package client/ (voir ClientSetup) et on n'y touche ici que
 * via une reference de methode vers une AUTRE classe (jamais un lambda inline qui
 * capturerait des types client localement).
 */
@Mod(UltimateZooTame.MODID)
public class UltimateZooTame {

    public static final String MODID = "ultimatezootaming";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltimateZooTame() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Config TOML client (voir config/ZooClientConfig.java)
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ZooClientConfig.SPEC, "ultimatezootame-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, com.lex3d.ultimatezootaming.config.ZooServerConfig.SPEC, "ultimatezootame-server.toml");

        // Registres (items, blocs, block entities, onglet créatif)
        ModItems.ITEMS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        com.lex3d.ultimatezootaming.core.init.ModMenuTypes.MENUS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        // Le forge bus sert pour les events gameplay (taming, mort, naissance...)
        // -> déjà géré par @Mod.EventBusSubscriber dans le package events/

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::registerConfigScreen);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
        LOGGER.info("[UltimateZooTaming] Common setup termine.");
    }
}
