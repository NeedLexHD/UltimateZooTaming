package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.config.ConfigSyncManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ConfigSyncC2SPacket {

    private final List<String> forcedModIds;

    public ConfigSyncC2SPacket(List<String> forcedModIds) {
        this.forcedModIds = forcedModIds;
    }

    public static void encode(ConfigSyncC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.forcedModIds.size());
        for (String id : packet.forcedModIds) {
            buf.writeUtf(id);
        }
    }

    public static ConfigSyncC2SPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readUtf());
        }
        return new ConfigSyncC2SPacket(list);
    }

    public static void handle(ConfigSyncC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null) {
                ConfigSyncManager.handleSync(ctx.getSender().getUUID(), packet.forcedModIds);
            }
        });
        ctx.setPacketHandled(true);
    }
}
