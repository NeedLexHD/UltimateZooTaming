package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class WhistleCommandC2SPacket {

    /**
     * GUARD = ancre le mob a sa position ACTUELLE, il erre librement dans le WanderRadius.
     * RELEASE = libere definitivement l'animal : il redevient sauvage (efface aussi le
     * taming natif vanilla si le mob en avait un) et quitte la liste du Sifflet.
     */
    public enum Command { SIT, FOLLOW, RECALL, GUARD, RELEASE }

    private final UUID familiarUUID;
    private final Command command;

    public WhistleCommandC2SPacket(UUID familiarUUID, Command command) {
        this.familiarUUID = familiarUUID;
        this.command = command;
    }

    public static void encode(WhistleCommandC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.familiarUUID);
        buf.writeEnum(packet.command);
    }

    public static WhistleCommandC2SPacket decode(FriendlyByteBuf buf) {
        UUID uuid = buf.readUUID();
        Command cmd = buf.readEnum(Command.class);
        return new WhistleCommandC2SPacket(uuid, cmd);
    }

    public static void handle(WhistleCommandC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() == null) return;
            ServerLevel level = ctx.getSender().serverLevel();
            Entity entity = level.getEntity(packet.familiarUUID);
            if (!(entity instanceof LivingEntity living)) return;

            living.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
                if (!ctx.getSender().getUUID().equals(data.getOwnerUUID())) return;

                switch (packet.command) {
                    case SIT -> data.setSitting(!data.isSitting());
                    case FOLLOW -> {
                        data.setSitting(false);
                        data.setGuardPos(null);
                    }
                    case RECALL -> {
                        data.setSitting(false);
                        data.setGuardPos(null);
                        living.teleportTo(ctx.getSender().getX(), ctx.getSender().getY(), ctx.getSender().getZ());
                    }
                    case GUARD -> data.setGuardPos(living.blockPosition());
                    case RELEASE -> {
                        // Retour a la vie sauvage : efface NOTRE taming...
                        data.setSitting(false);
                        data.setGuardPos(null);
                        data.setForcedTame(false);
                        data.setTrust(25f); // il garde un vague souvenir de toi
                        data.setOwnerUUID(null);
                        // ...ET le taming natif vanilla s'il existait (loup, chat, etc.)
                        if (living instanceof TamableAnimal tamable) {
                            tamable.setTame(false);
                            tamable.setOwnerUUID(null);
                            tamable.setOrderedToSit(false);
                        }
                        ZooSavedData.get(level).removeFamiliar(ctx.getSender().getUUID(), living.getUUID());

                        level.sendParticles(ParticleTypes.POOF,
                                living.getX(), living.getY() + living.getBbHeight() / 2.0, living.getZ(),
                                12, 0.3, 0.3, 0.3, 0.02);
                        level.playSound(null, living.blockPosition(), SoundEvents.CANDLE_EXTINGUISH,
                                SoundSource.NEUTRAL, 1.0f, 0.9f);
                    }
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
