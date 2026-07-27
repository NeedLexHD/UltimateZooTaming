package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Le client change le TYPE d'une zone (enclos / repos / vente / stockage). */
public class SetZoneTypeC2SPacket {

    private final UUID zoneId;
    private final int type;

    public SetZoneTypeC2SPacket(UUID zoneId, int type) {
        this.zoneId = zoneId;
        this.type = type;
    }

    public static void encode(SetZoneTypeC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.zoneId);
        buf.writeInt(packet.type);
    }

    public static SetZoneTypeC2SPacket decode(FriendlyByteBuf buf) {
        return new SetZoneTypeC2SPacket(buf.readUUID(), buf.readInt());
    }

    public static void handle(SetZoneTypeC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ZooZone zone = ZooSavedData.get(player.serverLevel()).getZone(packet.zoneId);
            if (zone == null) return;
            zone.setZoneType(packet.type);
            ZooSavedData.get(player.serverLevel()).markChanged();
        });
        ctx.setPacketHandled(true);
    }
}
