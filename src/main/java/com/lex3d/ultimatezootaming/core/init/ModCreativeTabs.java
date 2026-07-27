package com.lex3d.ultimatezootaming.core.init;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UltimateZooTame.MODID);

    public static final RegistryObject<CreativeModeTab> ZOO_TAB = CREATIVE_TABS.register("zoo_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.ultimatezootaming.zoo_tab"))
                    .icon(() -> new ItemStack(ModItems.WHISTLE.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.WHISTLE.get());
                        output.accept(ModBlocks.NET_SMALL.get());
                        output.accept(ModBlocks.NET_REINFORCED.get());
                        output.accept(ModBlocks.NET_POOL.get());
                        output.accept(ModItems.OCCUPIED_CONTAINER.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.ZOO_GUIDE.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.FODDER.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.FODDER_MEAT.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.FODDER_FISH.get());
                        output.accept(ModBlocks.ZOO_SIGN.get());
                        output.accept(ModBlocks.ZOO_ENTRANCE.get());
                        output.accept(ModBlocks.ZOO_PATH.get());
                        output.accept(ModBlocks.RECRUITMENT.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.PLOT_SELECTOR.get());
                        output.accept(ModBlocks.PHOTO_SPOT.get());
                        output.accept(ModBlocks.FEED_STATION.get());
                        output.accept(ModBlocks.WATER_JET.get());
                        output.accept(ModBlocks.ZOO_BENCH.get());
                        output.accept(ModBlocks.ZOO_BIN.get());
                        output.accept(ModBlocks.ZOO_VAULT.get());
                        output.accept(ModBlocks.CASH_REGISTER.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.DIRECTOR_TABLET.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.ZOO_MAP.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.SUPER_REMEDY.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.PARK_TICKET.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.UMBRELLA.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.KAWAII_UMBRELLA.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModBlocks.TICKET_BOOTH.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModBlocks.INCUBATOR.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModBlocks.TOY_BALL.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModBlocks.TOY_BAMBOO.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModBlocks.TOY_BRANCH.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModBlocks.TOY_TIRE.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.BINOCULARS.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.SELFIE_STICK.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.SAMPLING_SYRINGE.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.GENETIC_SAMPLE.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.ZOO_CAP.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.ZOO_BADGE.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.POPCORN.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.ICE_CREAM.get());
                        output.accept(com.lex3d.ultimatezootaming.core.init.ModItems.LEMONADE.get());
                        output.accept(ModBlocks.KEEPER_LOCKER.get());
                        output.accept(ModBlocks.KEEPER_LOCKER_GREEN.get());
                        output.accept(ModBlocks.KEEPER_LOCKER_BLUE.get());
                        output.accept(ModBlocks.KEEPER_LOCKER_RED.get());
                        output.accept(ModBlocks.KEEPER_LOCKER_YELLOW.get());
                        output.accept(ModBlocks.FEEDER.get());
                        output.accept(ModBlocks.FEEDER_STONE.get());
                        output.accept(ModBlocks.FEEDER_ICE.get());
                        output.accept(ModBlocks.FEEDER_SANDSTONE.get());
                        output.accept(ModBlocks.FEEDER_JUNGLE.get());
                        output.accept(ModBlocks.FEEDER_NETHER.get());
                        output.accept(ModItems.SURVEYOR_STAFF.get());
                        output.accept(ModItems.ANIMAL_REMEDY.get());
                        output.accept(ModItems.ZOO_KEEPER_SPAWN.get());

                        output.accept(ModItems.KIBBLE_CARNIVORE_BASIQUE.get());
                        output.accept(ModItems.KIBBLE_CARNIVORE_SUPERIEUR.get());
                        output.accept(ModItems.KIBBLE_CARNIVORE_APEX.get());
                        output.accept(ModItems.KIBBLE_HERBIVORE_BASIQUE.get());
                        output.accept(ModItems.KIBBLE_HERBIVORE_SUPERIEUR.get());
                        output.accept(ModItems.KIBBLE_HERBIVORE_APEX.get());
                        output.accept(ModItems.KIBBLE_PISCIVORE_BASIQUE.get());
                        output.accept(ModItems.KIBBLE_PISCIVORE_SUPERIEUR.get());
                        output.accept(ModItems.KIBBLE_PISCIVORE_APEX.get());

                        output.accept(ModBlocks.TRAPPING_CAGE_SMALL.get());
                        output.accept(ModBlocks.TRAPPING_CAGE_MEDIUM.get());
                        output.accept(ModBlocks.TRAPPING_CAGE_LARGE.get());
                        output.accept(ModBlocks.TRAPPING_CAGE_UNBREAKABLE.get());
                    })
                    .build());
}
