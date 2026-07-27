package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncMarketingS2CPacket {
    public final int highestRank, activeCampaign, campaignDaysLeft;
    public SyncMarketingS2CPacket(int r, int a, int d) {
        highestRank = r; activeCampaign = a; campaignDaysLeft = d;
    }
    public static void encode(SyncMarketingS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.highestRank); buf.writeInt(p.activeCampaign); buf.writeInt(p.campaignDaysLeft);
    }
    public static SyncMarketingS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncMarketingS2CPacket(buf.readInt(), buf.readInt(), buf.readInt());
    }
    public static void handle(SyncMarketingS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.openMarketing(packet)));
        ctx.setPacketHandled(true);
    }
}
