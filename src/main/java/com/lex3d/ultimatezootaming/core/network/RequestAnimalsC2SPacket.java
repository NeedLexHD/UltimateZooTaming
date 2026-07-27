package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Le client demande la fiche detaillee des animaux d'un enclos. */
public class RequestAnimalsC2SPacket {

    private final UUID zoneId;

    public RequestAnimalsC2SPacket(UUID zoneId) { this.zoneId = zoneId; }

    public static void encode(RequestAnimalsC2SPacket p, FriendlyByteBuf buf) { buf.writeUUID(p.zoneId); }

    public static RequestAnimalsC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestAnimalsC2SPacket(buf.readUUID());
    }

    public static void handle(RequestAnimalsC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ZooZone zone = ZooSavedData.get(player.serverLevel()).getZone(packet.zoneId);
            if (zone == null) return;

            List<SyncAnimalsS2CPacket.AnimalInfo> list = new ArrayList<>();
            for (Animal a : player.serverLevel().getEntitiesOfClass(Animal.class, zone.boundingBox(),
                    an -> an.isAlive() && zone.contains(an.blockPosition())
                            && an.getCapability(CapabilityHandler.TAMING_DATA)
                                .resolve().map(TamingData::isTamed).orElse(false))) {
                // Un chien ou un chat qui traverse l'enclos n'est pas un pensionnaire
                if (com.lex3d.ultimatezootaming.capability.PetSpecies.isPet(a)) continue;
                TamingData d = a.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
                if (d == null) continue;
                list.add(SyncAnimalsS2CPacket.describe(a, d, player.serverLevel()));
                if (list.size() >= 40) break; // securite paquet
            }
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncAnimalsS2CPacket(packet.zoneId, zone.getName(), list));
        });
        ctx.setPacketHandled(true);
    }
}
