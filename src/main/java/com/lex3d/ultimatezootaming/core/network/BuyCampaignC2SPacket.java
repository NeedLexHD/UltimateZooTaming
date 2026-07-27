package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.marketing.AdCampaign;
import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le joueur achete une campagne publicitaire. */
public class BuyCampaignC2SPacket {
    private final int campaignOrdinal;
    public BuyCampaignC2SPacket(int c) { this.campaignOrdinal = c; }
    public static void encode(BuyCampaignC2SPacket p, FriendlyByteBuf buf) { buf.writeInt(p.campaignOrdinal); }
    public static BuyCampaignC2SPacket decode(FriendlyByteBuf buf) { return new BuyCampaignC2SPacket(buf.readInt()); }

    public static void handle(BuyCampaignC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            var vals = AdCampaign.values();
            if (packet.campaignOrdinal <= 0 || packet.campaignOrdinal >= vals.length) return;
            var campaign = vals[packet.campaignOrdinal];
            var ledger = ZooLedger.get(level);
            // Verifier rang minimum
            if (!campaign.isUnlocked(ledger.getHighestRank())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.campaign_locked"), true);
                return;
            }
            // Verifier deja une campagne active
            if (ledger.getCampaignDaysLeft() > 0) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.campaign_active"), true);
                return;
            }
            // Verifier fonds
            if (!ZooVaultBlock.withdrawFromTreasury(level, campaign.cost)) {
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "message.ultimatezootaming.campaign_poor", campaign.cost), true);
                return;
            }
            ledger.startCampaign(campaign);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u2708 ")
                    .withStyle(net.minecraft.ChatFormatting.GOLD)
                    .append(net.minecraft.network.chat.Component.translatable(
                            "message.ultimatezootaming.campaign_started",
                            net.minecraft.network.chat.Component.translatable(
                                    "campaign.ultimatezootaming." + campaign.key))
                            .withStyle(net.minecraft.ChatFormatting.YELLOW)), false);
        });
        ctx.setPacketHandled(true);
    }
}
