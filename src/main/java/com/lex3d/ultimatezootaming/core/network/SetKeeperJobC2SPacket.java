package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cycle le metier d'un soigneur depuis le GUI (comme sneak+clic). */
public class SetKeeperJobC2SPacket {

    private final int keeperId;

    public SetKeeperJobC2SPacket(int keeperId) { this.keeperId = keeperId; }

    public static void encode(SetKeeperJobC2SPacket p, FriendlyByteBuf buf) { buf.writeInt(p.keeperId); }

    public static SetKeeperJobC2SPacket decode(FriendlyByteBuf buf) {
        return new SetKeeperJobC2SPacket(buf.readInt());
    }

    public static void handle(SetKeeperJobC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.serverLevel().getEntity(packet.keeperId) instanceof ZooKeeperEntity keeper) {
                keeper.setJob(keeper.getJob() + 1);
            }
        });
        ctx.setPacketHandled(true);
    }
}
