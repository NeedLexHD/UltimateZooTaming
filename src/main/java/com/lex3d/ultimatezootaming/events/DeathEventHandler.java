package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class DeathEventHandler {

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        LivingEntity entity = event.getEntity();

        entity.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            if (data.isTamed()) {
                ZooSavedData.get((ServerLevel) entity.level())
                        .removeFamiliar(data.getOwnerUUID(), entity.getUUID());
                // Purge l'etat d'evasion (evite une alerte fantome apres la mort)
                if (data.isEscaped()) {
                    data.setEscaped(false);
                    com.lex3d.ultimatezootaming.events.EscapeHandler.onRecapture(
                            (ServerLevel) entity.level(), entity);
                }
            }
        });
    }
}
