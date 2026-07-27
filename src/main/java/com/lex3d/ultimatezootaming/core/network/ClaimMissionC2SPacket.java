package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Le joueur reclame la prime d'une mission accomplie. */
public class ClaimMissionC2SPacket {

    private final int slot; // 0,1,2

    public ClaimMissionC2SPacket(int slot) { this.slot = slot; }

    public static void encode(ClaimMissionC2SPacket p, FriendlyByteBuf buf) { buf.writeInt(p.slot); }
    public static ClaimMissionC2SPacket decode(FriendlyByteBuf buf) {
        return new ClaimMissionC2SPacket(buf.readInt());
    }

    public static void handle(ClaimMissionC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            ZooLedger ledger = ZooLedger.get(level);
            int reward = ledger.claimMission(packet.slot);
            if (reward > 0) {
                var vault = ZooVaultBlock.anyVault(level);
                if (vault != null) vault.deposit(reward);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("+" + reward + " Ƶ ")
                        .withStyle(net.minecraft.ChatFormatting.GREEN)
                        .append(net.minecraft.network.chat.Component.translatable(
                                "message.ultimatezootaming.mission_claimed")
                                .withStyle(net.minecraft.ChatFormatting.YELLOW)), false);
            }
            // Re-sync la liste
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncMissionsS2CPacket(ledger.getMissions(), ledger.getMissionProgress(),
                            ledger.getMissionClaimed()));
        });
        ctx.setPacketHandled(true);
    }
}
