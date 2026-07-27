package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Libere un animal depuis le tableau de bord : il redevient sauvage et quitte l'enclos. */
public class ReleaseAnimalC2SPacket {

    private final int entityId;

    public ReleaseAnimalC2SPacket(int entityId) { this.entityId = entityId; }

    public static void encode(ReleaseAnimalC2SPacket p, FriendlyByteBuf buf) { buf.writeInt(p.entityId); }

    public static ReleaseAnimalC2SPacket decode(FriendlyByteBuf buf) {
        return new ReleaseAnimalC2SPacket(buf.readInt());
    }

    public static void handle(ReleaseAnimalC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Entity e = player.serverLevel().getEntity(packet.entityId);
            if (!(e instanceof Animal animal)) return;

            animal.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
                data.setOwnerUUID(null);   // isTamed() en derive : il redevient sauvage
                data.setForcedTame(false);
                data.setZoneId(null);
                data.setTrust(0);
                data.setSatisfaction(50);
                data.setSick(false);
            });
            // Petit effet + message
            player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    animal.getX(), animal.getY() + 0.5, animal.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            player.serverLevel().playSound(null, animal.blockPosition(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.4f, 0.8f);
            player.displayClientMessage(Component.translatable(
                    "message.ultimatezootaming.animal_released",
                    Component.translatable(animal.getType().getDescriptionId())), true);
        });
        ctx.setPacketHandled(true);
    }
}
