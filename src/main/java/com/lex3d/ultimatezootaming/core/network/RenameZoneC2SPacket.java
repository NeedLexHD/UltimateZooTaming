package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Le client renomme un enclos depuis le GUI. */
public class RenameZoneC2SPacket {

    private final UUID zoneId;
    private final String newName;

    public RenameZoneC2SPacket(UUID zoneId, String newName) {
        this.zoneId = zoneId;
        this.newName = newName;
    }

    public static void encode(RenameZoneC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.zoneId);
        buf.writeUtf(packet.newName);
    }

    public static RenameZoneC2SPacket decode(FriendlyByteBuf buf) {
        return new RenameZoneC2SPacket(buf.readUUID(), buf.readUtf());
    }

    public static void handle(RenameZoneC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ZooZone zone = ZooSavedData.get(player.serverLevel()).getZone(packet.zoneId);
            // Securite : seul le proprietaire peut renommer
            if (zone == null) return; // multi : zoo gere en commun
            String clean = packet.newName.trim();
            if (clean.isEmpty() || clean.length() > 32) return;
            zone.setName(clean);
            ZooSavedData.get(player.serverLevel()).markChanged();
        });
        ctx.setPacketHandled(true);
    }
}
