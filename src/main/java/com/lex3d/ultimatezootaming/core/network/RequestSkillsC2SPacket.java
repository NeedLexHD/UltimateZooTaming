package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Le client demande la fiche de competences d'un employe. */
public class RequestSkillsC2SPacket {

    private final int entityId;

    public RequestSkillsC2SPacket(int entityId) { this.entityId = entityId; }

    public static void encode(RequestSkillsC2SPacket p, FriendlyByteBuf buf) { buf.writeInt(p.entityId); }
    public static RequestSkillsC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestSkillsC2SPacket(buf.readInt());
    }

    public static void handle(RequestSkillsC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (!(player.serverLevel().getEntity(packet.entityId) instanceof ZooKeeperEntity k)) return;
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SyncSkillsS2CPacket(k.getId(), k.getName().getString(), k.getJob(),
                            k.getKeeperLevel(), k.getXp(), k.getFreePoints(), k.getSkillRanks()));
        });
        ctx.setPacketHandled(true);
    }
}
