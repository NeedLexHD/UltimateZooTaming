package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class RequestFamiliarsC2SPacket {

    public static void encode(RequestFamiliarsC2SPacket packet, FriendlyByteBuf buf) {
        // vide, aucune donnee a envoyer
    }

    public static RequestFamiliarsC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestFamiliarsC2SPacket();
    }

    private static String zoneNameOf(net.minecraft.server.level.ServerLevel level,
                                     com.lex3d.ultimatezootaming.capability.TamingData data) {
        if (data.getZoneId() == null) return "";
        var zone = com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getZone(data.getZoneId());
        return zone == null ? "" : zone.getName();
    }

    public static void handle(RequestFamiliarsC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            var familiarsUUIDs = ZooSavedData.get(player.serverLevel()).getFamiliars(player.getUUID());
            List<SyncFamiliarsS2CPacket.FamiliarInfo> infos = new ArrayList<>();

            for (UUID uuid : familiarsUUIDs) {
                Entity entity = player.serverLevel().getEntity(uuid);
                if (entity instanceof net.minecraft.world.entity.LivingEntity living && living.isAlive()) {
                    living.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data ->
                            infos.add(new SyncFamiliarsS2CPacket.FamiliarInfo(
                                    uuid,
                                    living.getType().getDescriptionId(),
                                    data.getTrust(),
                                    data.getWanderRadius(),
                                    data.isSitting(),
                                    data.isGuarding(),
                                    living.getX(), living.getY(), living.getZ(),
                                    data.getSatisfaction(), data.isSick(),
                                    zoneNameOf(player.serverLevel(), data)
                            )));
                }
            }

            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncFamiliarsS2CPacket(infos));
        });
        ctx.setPacketHandled(true);
    }
}
