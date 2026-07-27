package com.lex3d.ultimatezootaming.events;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.core.network.FamiliarBadgeS2CPacket;
import com.lex3d.ultimatezootaming.core.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Toutes les ~1s, pousse a chaque joueur l'etat "badge" (trait + malade) des
 * familiers proches, pour que le client dessine une icone au-dessus d'eux.
 */
@Mod.EventBusSubscriber(modid = UltimateZooTame.MODID)
public class BadgeSyncHandler {

    private static final int SYNC_INTERVAL = 20; // 1s
    private static final double RANGE = 48.0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % SYNC_INTERVAL != 0) return;

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            List<FamiliarBadgeS2CPacket.Badge> badges = new ArrayList<>();
            for (LivingEntity mob : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(RANGE))) {
                mob.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
                    if (!data.isTamed()) return;
                    TamingData.Trait trait = data.getTrait();
                    boolean showTrait = trait != TamingData.Trait.NONE;
                    if (showTrait || data.isSick()) {
                        badges.add(new FamiliarBadgeS2CPacket.Badge(
                                mob.getId(), trait.ordinal(), data.isSick()));
                    }
                });
            }
            if (!badges.isEmpty()) {
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new FamiliarBadgeS2CPacket(badges));
            }
        }
    }
}
