package com.lex3d.ultimatezootaming.core.init;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.FeederBlock;
import com.lex3d.ultimatezootaming.blocks.NetTrapBlock;
import com.lex3d.ultimatezootaming.blocks.TrappingCageBlock;
import com.lex3d.ultimatezootaming.items.NetTrapBlockItem;
import com.lex3d.ultimatezootaming.items.TrappingCageBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, UltimateZooTame.MODID);

    public static final RegistryObject<Block> TRAPPING_CAGE_SMALL = registerCage(
            "trapping_cage_small", TrappingCageBlock.CageSize.SMALL, 2.0f);
    public static final RegistryObject<Block> TRAPPING_CAGE_MEDIUM = registerCage(
            "trapping_cage_medium", TrappingCageBlock.CageSize.MEDIUM, 3.0f);
    public static final RegistryObject<Block> TRAPPING_CAGE_LARGE = registerCage(
            "trapping_cage_large", TrappingCageBlock.CageSize.LARGE, 4.0f);
    public static final RegistryObject<Block> TRAPPING_CAGE_UNBREAKABLE = registerCage(
            "trapping_cage_unbreakable", TrappingCageBlock.CageSize.UNBREAKABLE, -1.0f);

    // Filets : posables dans/pres de l'eau (waterlogged), meme mecanique d'appat que la cage.
    public static final RegistryObject<Block> NET_SMALL = registerNet("net_small", NetTrapBlock.NetTier.SMALL, 1.0f);
    public static final RegistryObject<Block> NET_REINFORCED = registerNet("net_reinforced", NetTrapBlock.NetTier.REINFORCED, 1.5f);
    public static final RegistryObject<Block> NET_POOL = registerNet("net_pool", NetTrapBlock.NetTier.POOL, 2.0f);

    // Mangeoire : reproduction passive des familiers dans un rayon de 8 blocs.
    public static final RegistryObject<Block> FEEDER = BLOCKS.register("feeder", () -> new FeederBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    public static final RegistryObject<Block> FEEDER_STONE = BLOCKS.register("feeder_stone", () -> new FeederBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    private static final RegistryObject<Item> FEEDER_STONE_ITEM = ModItems.ITEMS.register("feeder_stone",
            () -> new BlockItem(FEEDER_STONE.get(), new Item.Properties()));

    public static final RegistryObject<Block> FEEDER_ICE = BLOCKS.register("feeder_ice", () -> new FeederBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    private static final RegistryObject<Item> FEEDER_ICE_ITEM = ModItems.ITEMS.register("feeder_ice",
            () -> new BlockItem(FEEDER_ICE.get(), new Item.Properties()));

    public static final RegistryObject<Block> FEEDER_SANDSTONE = BLOCKS.register("feeder_sandstone", () -> new FeederBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    private static final RegistryObject<Item> FEEDER_SANDSTONE_ITEM = ModItems.ITEMS.register("feeder_sandstone",
            () -> new BlockItem(FEEDER_SANDSTONE.get(), new Item.Properties()));

    public static final RegistryObject<Block> FEEDER_JUNGLE = BLOCKS.register("feeder_jungle", () -> new FeederBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    private static final RegistryObject<Item> FEEDER_JUNGLE_ITEM = ModItems.ITEMS.register("feeder_jungle",
            () -> new BlockItem(FEEDER_JUNGLE.get(), new Item.Properties()));

    public static final RegistryObject<Block> FEEDER_NETHER = BLOCKS.register("feeder_nether", () -> new FeederBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));
    private static final RegistryObject<Item> FEEDER_NETHER_ITEM = ModItems.ITEMS.register("feeder_nether",
            () -> new BlockItem(FEEDER_NETHER.get(), new Item.Properties()));

    public static final RegistryObject<Block> ZOO_PATH = BLOCKS.register("zoo_path",
            () -> new com.lex3d.ultimatezootaming.blocks.ZooPathBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.2f)
                            .sound(SoundType.STONE)));
    private static final RegistryObject<Item> ZOO_PATH_ITEM = ModItems.ITEMS.register("zoo_path",
            () -> new BlockItem(ZOO_PATH.get(), new Item.Properties()));

    public static final RegistryObject<Block> RECRUITMENT = BLOCKS.register("recruitment_computer",
            () -> new com.lex3d.ultimatezootaming.blocks.RecruitmentBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.8f)
                            .sound(SoundType.METAL).noOcclusion()));
    private static final RegistryObject<Item> RECRUITMENT_ITEM = ModItems.ITEMS.register("recruitment_computer",
            () -> new BlockItem(RECRUITMENT.get(), new Item.Properties()));

    public static final RegistryObject<Block> PHOTO_SPOT = BLOCKS.register("photo_spot",
            () -> new com.lex3d.ultimatezootaming.blocks.InteractionStationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5f)
                            .sound(SoundType.METAL).noOcclusion(),
                    com.lex3d.ultimatezootaming.blocks.InteractionStationBlock.Kind.PHOTO));
    private static final RegistryObject<Item> PHOTO_SPOT_ITEM = ModItems.ITEMS.register("photo_spot",
            () -> new BlockItem(PHOTO_SPOT.get(), new Item.Properties()));



    // Jouet d'enrichissement : ball
    public static final RegistryObject<Block> TOY_BALL = BLOCKS.register("toy_ball",
            () -> new com.lex3d.ultimatezootaming.blocks.EnrichmentBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.4f)
                            .sound(SoundType.WOOL).noOcclusion().noCollission(),
                    com.lex3d.ultimatezootaming.blocks.EnrichmentBlock.Kind.BALL));
    private static final RegistryObject<Item> TOY_BALL_ITEM = ModItems.ITEMS.register("toy_ball",
            () -> new BlockItem(TOY_BALL.get(), new Item.Properties()));
    // Jouet d'enrichissement : bamboo
    public static final RegistryObject<Block> TOY_BAMBOO = BLOCKS.register("toy_bamboo",
            () -> new com.lex3d.ultimatezootaming.blocks.EnrichmentBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.4f)
                            .sound(SoundType.WOOL).noOcclusion().noCollission(),
                    com.lex3d.ultimatezootaming.blocks.EnrichmentBlock.Kind.BAMBOO));
    private static final RegistryObject<Item> TOY_BAMBOO_ITEM = ModItems.ITEMS.register("toy_bamboo",
            () -> new BlockItem(TOY_BAMBOO.get(), new Item.Properties()));
    // Jouet d'enrichissement : branch
    public static final RegistryObject<Block> TOY_BRANCH = BLOCKS.register("toy_branch",
            () -> new com.lex3d.ultimatezootaming.blocks.EnrichmentBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.4f)
                            .sound(SoundType.WOOL).noOcclusion().noCollission(),
                    com.lex3d.ultimatezootaming.blocks.EnrichmentBlock.Kind.BRANCH));
    private static final RegistryObject<Item> TOY_BRANCH_ITEM = ModItems.ITEMS.register("toy_branch",
            () -> new BlockItem(TOY_BRANCH.get(), new Item.Properties()));
    // Jouet d'enrichissement : tire
    public static final RegistryObject<Block> TOY_TIRE = BLOCKS.register("toy_tire",
            () -> new com.lex3d.ultimatezootaming.blocks.EnrichmentBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.4f)
                            .sound(SoundType.WOOL).noOcclusion().noCollission(),
                    com.lex3d.ultimatezootaming.blocks.EnrichmentBlock.Kind.TIRE));
    private static final RegistryObject<Item> TOY_TIRE_ITEM = ModItems.ITEMS.register("toy_tire",
            () -> new BlockItem(TOY_TIRE.get(), new Item.Properties()));

    // Dechet au sol : laisse par les visiteurs quand il manque des poubelles
    public static final RegistryObject<Block> LITTER = BLOCKS.register("litter",
            () -> new com.lex3d.ultimatezootaming.blocks.LitterBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE)
                            .strength(0.05f).sound(SoundType.WOOL).noOcclusion()
                            .noCollission().instabreak()));

    // Guichet de billetterie : cree une vraie file d'attente a l'entree du zoo
    public static final RegistryObject<Block> TICKET_BOOTH = BLOCKS.register("ticket_booth",
            () -> new com.lex3d.ultimatezootaming.blocks.TicketBoothBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f)
                            .sound(SoundType.WOOD).noOcclusion()));
    private static final RegistryObject<Item> TICKET_BOOTH_ITEM = ModItems.ITEMS.register("ticket_booth",
            () -> new BlockItem(TICKET_BOOTH.get(), new Item.Properties()));

    // Incubateur : reproduction selective avec heritage genetique
    public static final RegistryObject<Block> INCUBATOR = BLOCKS.register("incubator",
            () -> new com.lex3d.ultimatezootaming.blocks.IncubatorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(2.0f)
                            .sound(SoundType.METAL).noOcclusion()));
    private static final RegistryObject<Item> INCUBATOR_ITEM = ModItems.ITEMS.register("incubator",
            () -> new BlockItem(INCUBATOR.get(), new Item.Properties()));

    public static final RegistryObject<Block> FEED_STATION = BLOCKS.register("feed_station",
            () -> new com.lex3d.ultimatezootaming.blocks.InteractionStationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f)
                            .sound(SoundType.WOOD).noOcclusion(),
                    com.lex3d.ultimatezootaming.blocks.InteractionStationBlock.Kind.FEED));
    private static final RegistryObject<Item> FEED_STATION_ITEM = ModItems.ITEMS.register("feed_station",
            () -> new BlockItem(FEED_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Block> WATER_JET = BLOCKS.register("water_jet",
            () -> new com.lex3d.ultimatezootaming.blocks.InteractionStationBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.5f)
                            .sound(SoundType.METAL).noOcclusion(),
                    com.lex3d.ultimatezootaming.blocks.InteractionStationBlock.Kind.WATER));
    private static final RegistryObject<Item> WATER_JET_ITEM = ModItems.ITEMS.register("water_jet",
            () -> new BlockItem(WATER_JET.get(), new Item.Properties()));



    public static final RegistryObject<Block> ZOO_BENCH = BLOCKS.register("zoo_bench",
            () -> new com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock(
                    com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.Kind.BENCH,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f)
                            .sound(SoundType.WOOD).noOcclusion()));
    private static final RegistryObject<Item> ZOO_BENCH_ITEM = ModItems.ITEMS.register("zoo_bench",
            () -> new BlockItem(ZOO_BENCH.get(), new Item.Properties()));

    public static final RegistryObject<Block> ZOO_BIN = BLOCKS.register("zoo_bin",
            () -> new com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock(
                    com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.Kind.BIN,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(1.5f)
                            .sound(SoundType.METAL).noOcclusion()));
    private static final RegistryObject<Item> ZOO_BIN_ITEM = ModItems.ITEMS.register("zoo_bin",
            () -> new BlockItem(ZOO_BIN.get(), new Item.Properties()));

    public static final RegistryObject<Block> ZOO_ENTRANCE = BLOCKS.register("zoo_entrance",
            () -> new com.lex3d.ultimatezootaming.blocks.ZooEntranceBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f)
                            .sound(SoundType.WOOD).noOcclusion()));
    private static final RegistryObject<Item> ZOO_ENTRANCE_ITEM = ModItems.ITEMS.register("zoo_entrance",
            () -> new BlockItem(ZOO_ENTRANCE.get(), new Item.Properties()));

    public static final RegistryObject<Block> ZOO_VAULT = BLOCKS.register("zoo_vault",
            () -> new com.lex3d.ultimatezootaming.blocks.ZooVaultBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                            .sound(SoundType.METAL).noOcclusion()));
    private static final RegistryObject<Item> ZOO_VAULT_ITEM = ModItems.ITEMS.register("zoo_vault",
            () -> new BlockItem(ZOO_VAULT.get(), new Item.Properties()));

    public static final RegistryObject<Block> CASH_REGISTER = BLOCKS.register("cash_register",
            () -> new com.lex3d.ultimatezootaming.blocks.ShopBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f)
                            .sound(SoundType.METAL).noOcclusion()));
    private static final RegistryObject<Item> CASH_REGISTER_ITEM = ModItems.ITEMS.register("cash_register",
            () -> new BlockItem(CASH_REGISTER.get(), new Item.Properties()));

    public static final RegistryObject<Block> ZOO_SIGN = BLOCKS.register("zoo_sign",
            () -> new com.lex3d.ultimatezootaming.blocks.ZooSignBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(1.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));
    private static final RegistryObject<Item> ZOO_SIGN_ITEM = ModItems.ITEMS.register("zoo_sign",
            () -> new BlockItem(ZOO_SIGN.get(), new Item.Properties()));

    public static final RegistryObject<Block> KEEPER_LOCKER = BLOCKS.register("keeper_locker",
            () -> new com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD)));
    private static final RegistryObject<Item> KEEPER_LOCKER_ITEM = ModItems.ITEMS.register("keeper_locker",
            () -> new BlockItem(KEEPER_LOCKER.get(), new Item.Properties()));
    public static final RegistryObject<Block> KEEPER_LOCKER_GREEN = BLOCKS.register("keeper_locker_green",
            () -> new com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD), "green"));
    private static final RegistryObject<Item> KEEPER_LOCKER_GREEN_ITEM = ModItems.ITEMS.register("keeper_locker_green",
            () -> new BlockItem(KEEPER_LOCKER_GREEN.get(), new Item.Properties()));
    public static final RegistryObject<Block> KEEPER_LOCKER_YELLOW = BLOCKS.register("keeper_locker_yellow",
            () -> new com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD), "yellow"));
    private static final RegistryObject<Item> KEEPER_LOCKER_YELLOW_ITEM = ModItems.ITEMS.register("keeper_locker_yellow",
            () -> new BlockItem(KEEPER_LOCKER_YELLOW.get(), new Item.Properties()));
    public static final RegistryObject<Block> KEEPER_LOCKER_BLUE = BLOCKS.register("keeper_locker_blue",
            () -> new com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD), "blue"));
    private static final RegistryObject<Item> KEEPER_LOCKER_BLUE_ITEM = ModItems.ITEMS.register("keeper_locker_blue",
            () -> new BlockItem(KEEPER_LOCKER_BLUE.get(), new Item.Properties()));
    public static final RegistryObject<Block> KEEPER_LOCKER_RED = BLOCKS.register("keeper_locker_red",
            () -> new com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0f, 3.0f)
                            .sound(SoundType.WOOD), "red"));
    private static final RegistryObject<Item> KEEPER_LOCKER_RED_ITEM = ModItems.ITEMS.register("keeper_locker_red",
            () -> new BlockItem(KEEPER_LOCKER_RED.get(), new Item.Properties()));


    private static final RegistryObject<Item> FEEDER_ITEM = ModItems.ITEMS.register("feeder",
            () -> new BlockItem(FEEDER.get(), new Item.Properties()));

    private static RegistryObject<Block> registerCage(String name, TrappingCageBlock.CageSize size, float hardness) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new TrappingCageBlock(size,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .strength(hardness, size.isUnbreakable() ? 3600000.0f : hardness * 3)
                        .sound(SoundType.METAL)
                        .noOcclusion()
                        // Traversable comme une toile d'araignee : le mob peut physiquement
                        // marcher DANS la cage (pas bloque comme un mur), ce qui permet a
                        // entityInside de se declencher normalement au sol, sans avoir a
                        // encastrer le bloc. C'est ce qui manquait pour l'effet "plaque
                        // de pression" attendu.
                        .noCollission()));

        // TrappingCageBlockItem place physiquement tout le footprint N x N (voir la classe),
        // pas un simple BlockItem qui ne poserait qu'une seule cellule.
        ModItems.ITEMS.register(name, () -> new TrappingCageBlockItem((TrappingCageBlock) block.get(),
                new Item.Properties().durability(size.isUnbreakable() ? 10 : 3)));
        return block;
    }

    private static RegistryObject<Block> registerNet(String name, NetTrapBlock.NetTier tier, float hardness) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> new NetTrapBlock(tier,
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.WATER)
                        .strength(hardness, hardness * 2)
                        .sound(SoundType.WOOL)
                        .noOcclusion()
                        .noCollission()));

        // NetTrapBlockItem : permet de poser le filet en cliquant directement SUR l'eau
        // (raytrace des fluides, comme le nenuphar) -- voir la classe pour le detail.
        ModItems.ITEMS.register(name, () -> new NetTrapBlockItem((NetTrapBlock) block.get(),
                new Item.Properties().durability(3)));
        return block;
    }
}
