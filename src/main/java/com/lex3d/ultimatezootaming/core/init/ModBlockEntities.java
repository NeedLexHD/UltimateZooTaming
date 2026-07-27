package com.lex3d.ultimatezootaming.core.init;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.FeederBlockEntity;
import com.lex3d.ultimatezootaming.blocks.NetTrapBlockEntity;
import com.lex3d.ultimatezootaming.blocks.TrappingCageBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, UltimateZooTame.MODID);

    public static final RegistryObject<BlockEntityType<TrappingCageBlockEntity>> TRAPPING_CAGE =
            BLOCK_ENTITIES.register("trapping_cage", () -> BlockEntityType.Builder.of(
                    TrappingCageBlockEntity::new,
                    ModBlocks.TRAPPING_CAGE_SMALL.get(),
                    ModBlocks.TRAPPING_CAGE_MEDIUM.get(),
                    ModBlocks.TRAPPING_CAGE_LARGE.get(),
                    ModBlocks.TRAPPING_CAGE_UNBREAKABLE.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<FeederBlockEntity>> FEEDER =
            BLOCK_ENTITIES.register("feeder", () -> BlockEntityType.Builder.of(
                    FeederBlockEntity::new,
                    ModBlocks.FEEDER.get(),
                    ModBlocks.FEEDER_STONE.get(),
                    ModBlocks.FEEDER_ICE.get(),
                    ModBlocks.FEEDER_SANDSTONE.get(),
                    ModBlocks.FEEDER_JUNGLE.get(),
                    ModBlocks.FEEDER_NETHER.get()
            ).build(null));

    public static final RegistryObject<BlockEntityType<com.lex3d.ultimatezootaming.blocks.KeeperLockerBlockEntity>> KEEPER_LOCKER =
            BLOCK_ENTITIES.register("keeper_locker", () -> BlockEntityType.Builder.of(
                    com.lex3d.ultimatezootaming.blocks.KeeperLockerBlockEntity::new,
                    ModBlocks.KEEPER_LOCKER.get(), ModBlocks.KEEPER_LOCKER_GREEN.get(), ModBlocks.KEEPER_LOCKER_BLUE.get(), ModBlocks.KEEPER_LOCKER_RED.get(), ModBlocks.KEEPER_LOCKER_YELLOW.get()
            ).build(null));


    public static final RegistryObject<BlockEntityType<com.lex3d.ultimatezootaming.blocks.DecorBlockEntity>> DECOR =
            BLOCK_ENTITIES.register("decor", () -> BlockEntityType.Builder.of(
                    com.lex3d.ultimatezootaming.blocks.DecorBlockEntity::new,
                    ModBlocks.ZOO_SIGN.get(), ModBlocks.ZOO_BENCH.get(), ModBlocks.ZOO_BIN.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.lex3d.ultimatezootaming.blocks.StationBlockEntity>> STATION =
            BLOCK_ENTITIES.register("station", () -> BlockEntityType.Builder.of(
                    com.lex3d.ultimatezootaming.blocks.StationBlockEntity::new,
                    ModBlocks.PHOTO_SPOT.get(), ModBlocks.FEED_STATION.get(), ModBlocks.WATER_JET.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.lex3d.ultimatezootaming.blocks.ZooEntranceBlockEntity>> ZOO_ENTRANCE =
            BLOCK_ENTITIES.register("zoo_entrance", () -> BlockEntityType.Builder.of(
                    com.lex3d.ultimatezootaming.blocks.ZooEntranceBlockEntity::new,
                    ModBlocks.ZOO_ENTRANCE.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity>> ZOO_VAULT =
            BLOCK_ENTITIES.register("zoo_vault", () -> BlockEntityType.Builder.of(
                    com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity::new,
                    ModBlocks.ZOO_VAULT.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.lex3d.ultimatezootaming.blocks.ShopBlockEntity>> SHOP =
            BLOCK_ENTITIES.register("shop", () -> BlockEntityType.Builder.of(
                    com.lex3d.ultimatezootaming.blocks.ShopBlockEntity::new,
                    ModBlocks.CASH_REGISTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<com.lex3d.ultimatezootaming.blocks.IncubatorBlockEntity>> INCUBATOR =
            BLOCK_ENTITIES.register("incubator", () -> BlockEntityType.Builder.of(
                    com.lex3d.ultimatezootaming.blocks.IncubatorBlockEntity::new,
                    ModBlocks.INCUBATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<NetTrapBlockEntity>> NET_TRAP =
            BLOCK_ENTITIES.register("net_trap", () -> BlockEntityType.Builder.of(
                    NetTrapBlockEntity::new,
                    ModBlocks.NET_SMALL.get(),
                    ModBlocks.NET_REINFORCED.get(),
                    ModBlocks.NET_POOL.get()
            ).build(null));

}
