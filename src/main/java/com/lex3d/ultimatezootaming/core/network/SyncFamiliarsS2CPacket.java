package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class SyncFamiliarsS2CPacket {

    public record FamiliarInfo(UUID uuid, String descriptionId, float trust, double wanderRadius,
                                boolean sitting, boolean guarding, double x, double y, double z,
                                int satisfaction, boolean sick, String zoneName) {}

    private final List<FamiliarInfo> familiars;

    public SyncFamiliarsS2CPacket(List<FamiliarInfo> familiars) {
        this.familiars = familiars;
    }

    public static void encode(SyncFamiliarsS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.familiars.size());
        for (FamiliarInfo info : packet.familiars) {
            buf.writeUUID(info.uuid());
            buf.writeUtf(info.descriptionId());
            buf.writeFloat(info.trust());
            buf.writeDouble(info.wanderRadius());
            buf.writeBoolean(info.sitting());
            buf.writeBoolean(info.guarding());
            buf.writeDouble(info.x());
            buf.writeDouble(info.y());
            buf.writeDouble(info.z());
            buf.writeInt(info.satisfaction());
            buf.writeBoolean(info.sick());
            buf.writeUtf(info.zoneName());
        }
    }

    public static SyncFamiliarsS2CPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<FamiliarInfo> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(new FamiliarInfo(buf.readUUID(), buf.readUtf(), buf.readFloat(), buf.readDouble(),
                    buf.readBoolean(), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readInt(), buf.readBoolean(), buf.readUtf()));
        }
        return new SyncFamiliarsS2CPacket(list);
    }

    public static void handle(SyncFamiliarsS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.updateFamiliarsCache(packet.familiars)));
        ctx.setPacketHandled(true);
    }
}
