package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * Le client vise un familier -> demande son bien-etre au serveur (throttle cote
 * client, voir ClientForgeBusEvents). Le serveur repond avec WelfareInfoS2CPacket.
 * Evite de synchroniser en permanence tous les mobs : on ne pousse la donnee que
 * pour l'entite reellement regardee.
 */
public class RequestWelfareC2SPacket {

    private final int entityId;

    public RequestWelfareC2SPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(RequestWelfareC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.entityId);
    }

    public static RequestWelfareC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestWelfareC2SPacket(buf.readInt());
    }

    public static void handle(RequestWelfareC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Entity entity = player.serverLevel().getEntity(packet.entityId);
            if (!(entity instanceof LivingEntity living)) return;

            living.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
                if (!data.isTamed()) return;
                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                        new WelfareInfoS2CPacket(packet.entityId, data.getSatisfaction(),
                                data.isSick(), data.getZoneId() != null, data.getTrait().name()));
            });
        });
        ctx.setPacketHandled(true);
    }
}
