package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.core.init.ModEntities;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Enregistre les attributs (PV, vitesse...) des entites du mod. Bus MOD. */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {
    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ZOO_KEEPER.get(), ZooKeeperEntity.createAttributes().build());
        event.put(ModEntities.VISITOR.get(),
                com.lex3d.ultimatezootaming.entities.VisitorEntity.createAttributes().build());
    }
}
