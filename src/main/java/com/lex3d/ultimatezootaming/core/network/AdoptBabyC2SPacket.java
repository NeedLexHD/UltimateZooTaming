package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Envoye depuis MaternityScreen : "Permet de nommer le bebe et de lui donner la
 * TamingData, sinon il reste sauvage pour l'elevage de viande."
 */
public class AdoptBabyC2SPacket {

    private final UUID babyUUID;
    private final String customName;
    private final boolean adopt;

    public AdoptBabyC2SPacket(UUID babyUUID, String customName, boolean adopt) {
        this.babyUUID = babyUUID;
        this.customName = customName;
        this.adopt = adopt;
    }

    public static void encode(AdoptBabyC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.babyUUID);
        buf.writeUtf(packet.customName);
        buf.writeBoolean(packet.adopt);
    }

    public static AdoptBabyC2SPacket decode(FriendlyByteBuf buf) {
        return new AdoptBabyC2SPacket(buf.readUUID(), buf.readUtf(), buf.readBoolean());
    }

    public static void handle(AdoptBabyC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() == null || !packet.adopt) return;

            ServerLevel level = ctx.getSender().serverLevel();
            Entity entity = level.getEntity(packet.babyUUID);
            if (!(entity instanceof LivingEntity living)) return;

            if (!packet.customName.isBlank()) {
                living.setCustomName(Component.literal(packet.customName));
                living.setCustomNameVisible(true);
            }

            living.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
                data.setOwnerUUID(ctx.getSender().getUUID());
                data.setForcedTame(false);
                data.setTrust(100f);
                if (living instanceof Mob mob) {
                    mob.setPersistenceRequired();
                }
                ZooSavedData.get(level).addFamiliar(ctx.getSender().getUUID(), living.getUUID());
            });
        });
        ctx.setPacketHandled(true);
    }
}
