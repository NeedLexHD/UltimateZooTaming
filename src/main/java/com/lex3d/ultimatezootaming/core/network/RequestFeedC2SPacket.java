package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Le client ouvre le flux social. */
public class RequestFeedC2SPacket {

    public RequestFeedC2SPacket() {}
    public static void encode(RequestFeedC2SPacket p, FriendlyByteBuf buf) {}
    public static RequestFeedC2SPacket decode(FriendlyByteBuf buf) { return new RequestFeedC2SPacket(); }

    public static void handle(RequestFeedC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            var ledger = ZooLedger.get(player.serverLevel());
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncFeedS2CPacket(ledger.getFeed().getPosts(),
                            ledger.getFeed().moodPercent(ledger.getDay()),
                            (int) Math.round(ledger.buzzFactor() * 100),
                            ledger.getDay()));
        });
        ctx.setPacketHandled(true);
    }
}
