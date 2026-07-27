package com.lex3d.ultimatezootaming.core.init;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, UltimateZooTame.MODID);

    public static final RegistryObject<EntityType<ZooKeeperEntity>> ZOO_KEEPER =
            ENTITIES.register("zoo_keeper", () -> EntityType.Builder.of(ZooKeeperEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("zoo_keeper"));

    public static final RegistryObject<EntityType<com.lex3d.ultimatezootaming.entities.VisitorEntity>> VISITOR =
            ENTITIES.register("visitor", () -> EntityType.Builder.of(
                            com.lex3d.ultimatezootaming.entities.VisitorEntity::new, MobCategory.CREATURE)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build("visitor"));

    /** Ballon echappe : pur decor volant, sans IA ni collision. */
    public static final RegistryObject<EntityType<com.lex3d.ultimatezootaming.entities.LooseBalloonEntity>> LOOSE_BALLOON =
            ENTITIES.register("loose_balloon", () -> EntityType.Builder.<com.lex3d.ultimatezootaming.entities.LooseBalloonEntity>of(
                            com.lex3d.ultimatezootaming.entities.LooseBalloonEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.7f)
                    .clientTrackingRange(8)
                    .noSummon()
                    .build("loose_balloon"));
}
