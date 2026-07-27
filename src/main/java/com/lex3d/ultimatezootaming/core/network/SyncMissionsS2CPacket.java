package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Envoie les missions du jour au client (indices, progression, reclame). */
public class SyncMissionsS2CPacket {

    public final int[] missions;
    public final int[] progress;
    public final boolean[] claimed;

    public SyncMissionsS2CPacket(int[] missions, int[] progress, boolean[] claimed) {
        this.missions = missions;
        this.progress = progress;
        this.claimed = claimed;
    }

    public static void encode(SyncMissionsS2CPacket p, FriendlyByteBuf buf) {
        for (int i = 0; i < 3; i++) { buf.writeInt(p.missions[i]); buf.writeInt(p.progress[i]); buf.writeBoolean(p.claimed[i]); }
    }

    public static SyncMissionsS2CPacket decode(FriendlyByteBuf buf) {
        int[] m = new int[3]; int[] pr = new int[3]; boolean[] c = new boolean[3];
        for (int i = 0; i < 3; i++) { m[i] = buf.readInt(); pr[i] = buf.readInt(); c[i] = buf.readBoolean(); }
        return new SyncMissionsS2CPacket(m, pr, c);
    }

    public static void handle(SyncMissionsS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.openMissions(packet)));
        ctx.setPacketHandled(true);
    }
}
