package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Le client demande les infos marketing pour ouvrir le GUI. */
public class RequestMarketingC2SPacket {
    public RequestMarketingC2SPacket() {}
    public static void encode(RequestMarketingC2SPacket p, FriendlyByteBuf buf) {}
    public static RequestMarketingC2SPacket decode(FriendlyByteBuf buf) { return new RequestMarketingC2SPacket(); }
    public static void handle(RequestMarketingC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            var ledger = ZooLedger.get(player.serverLevel());
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncMarketingS2CPacket(ledger.getHighestRank(),
                            ledger.getActiveCampaign(), ledger.getCampaignDaysLeft()));
        });
        ctx.setPacketHandled(true);
    }
}
