package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.welfare.HabitatManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Envoie les overrides habitat/regime au client et ouvre le GUI admin. */
public class OpenHabitatGuiS2CPacket {

    private final Map<String, HabitatManager.Entry> overrides;

    public OpenHabitatGuiS2CPacket(Map<String, HabitatManager.Entry> overrides) {
        this.overrides = overrides;
    }

    public static void encode(OpenHabitatGuiS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.overrides.size());
        for (var e : p.overrides.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeByte(e.getValue().habitat());
            buf.writeByte(e.getValue().diet());
        }
    }

    public static OpenHabitatGuiS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        Map<String, HabitatManager.Entry> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            map.put(buf.readUtf(), new HabitatManager.Entry(buf.readByte(), buf.readByte()));
        }
        return new OpenHabitatGuiS2CPacket(map);
    }

    public static void handle(OpenHabitatGuiS2CPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.openHabitatScreen(p.overrides)));
        ctx.get().setPacketHandled(true);
    }
}
