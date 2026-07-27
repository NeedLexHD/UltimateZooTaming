package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Le client demande les 3 missions du jour + progression. */
public class RequestMissionsC2SPacket {

    public RequestMissionsC2SPacket() {}
    public static void encode(RequestMissionsC2SPacket p, FriendlyByteBuf buf) {}
    public static RequestMissionsC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestMissionsC2SPacket();
    }

    public static void handle(RequestMissionsC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            ZooLedger ledger = ZooLedger.get(level);
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncMissionsS2CPacket(ledger.getMissions(), ledger.getMissionProgress(),
                            ledger.getMissionClaimed()));
        });
        ctx.setPacketHandled(true);
    }
}
