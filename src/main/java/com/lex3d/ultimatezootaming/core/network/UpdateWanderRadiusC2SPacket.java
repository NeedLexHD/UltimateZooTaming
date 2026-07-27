package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class UpdateWanderRadiusC2SPacket {

    private final UUID familiarUUID;
    private final double radius;

    public UpdateWanderRadiusC2SPacket(UUID familiarUUID, double radius) {
        this.familiarUUID = familiarUUID;
        this.radius = radius;
    }

    public static void encode(UpdateWanderRadiusC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.familiarUUID);
        buf.writeDouble(packet.radius);
    }

    public static UpdateWanderRadiusC2SPacket decode(FriendlyByteBuf buf) {
        return new UpdateWanderRadiusC2SPacket(buf.readUUID(), buf.readDouble());
    }

    public static void handle(UpdateWanderRadiusC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() == null) return;
            ServerLevel level = ctx.getSender().serverLevel();
            Entity entity = level.getEntity(packet.familiarUUID);
            if (!(entity instanceof net.minecraft.world.entity.LivingEntity living)) return;

            living.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
                if (ctx.getSender().getUUID().equals(data.getOwnerUUID())) {
                    data.setWanderRadius(packet.radius);
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
