package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Bascule le zoo en PAUSE (ferme temporairement) ou le rouvre. */
public class PauseZooC2SPacket {

    public PauseZooC2SPacket() {}

    public static void encode(PauseZooC2SPacket p, FriendlyByteBuf buf) {}

    public static PauseZooC2SPacket decode(FriendlyByteBuf buf) {
        return new PauseZooC2SPacket();
    }

    public static void handle(PauseZooC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ServerLevel level = player.serverLevel();
            var ledger = com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level);
            boolean nowPaused = !ledger.isPaused();
            ledger.setPaused(nowPaused);
            player.displayClientMessage(Component.translatable(nowPaused
                    ? "message.ultimatezootaming.zoo_paused"
                    : "message.ultimatezootaming.zoo_resumed"), true);
        });
        ctx.setPacketHandled(true);
    }
}
