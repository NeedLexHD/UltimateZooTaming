package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.saveddata.ZooLedger;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le directeur change la politique du billet depuis le GUI. */
public class SetTicketPolicyC2SPacket {

    public SetTicketPolicyC2SPacket() {}

    public static void encode(SetTicketPolicyC2SPacket p, FriendlyByteBuf buf) {}

    public static SetTicketPolicyC2SPacket decode(FriendlyByteBuf buf) {
        return new SetTicketPolicyC2SPacket();
    }

    public static void handle(SetTicketPolicyC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ZooLedger.get(player.serverLevel()).cycleTicketPolicy();
        });
        ctx.setPacketHandled(true);
    }
}
