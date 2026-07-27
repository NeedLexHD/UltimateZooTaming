package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Remet le zoo a zero (jour 0, argent, stats, jalons) — bouton de test. */
public class ResetZooC2SPacket {

    public ResetZooC2SPacket() {}

    public static void encode(ResetZooC2SPacket p, FriendlyByteBuf buf) {}

    public static ResetZooC2SPacket decode(FriendlyByteBuf buf) {
        return new ResetZooC2SPacket();
    }

    public static void handle(ResetZooC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            // Seul un joueur op / createur peut reset (securite serveur)
            if (!player.hasPermissions(2) && !level.getServer().isSingleplayer()) {
                player.displayClientMessage(Component.translatable(
                        "message.ultimatezootaming.reset_denied"), true);
                return;
            }
            com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).resetAll();
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.reset_done"), true);
        });
        ctx.setPacketHandled(true);
    }
}
