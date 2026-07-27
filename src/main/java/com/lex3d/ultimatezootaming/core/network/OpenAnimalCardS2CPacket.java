package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le serveur envoie la fiche d'UN animal (sneak+clic sur l'animal). */
public class OpenAnimalCardS2CPacket {

    private final SyncAnimalsS2CPacket.AnimalInfo animal;

    public OpenAnimalCardS2CPacket(SyncAnimalsS2CPacket.AnimalInfo animal) { this.animal = animal; }

    public static void encode(OpenAnimalCardS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.animal.entityId());
        buf.writeUtf(p.animal.name());
        buf.writeUtf(p.animal.species());
        buf.writeInt(p.animal.welfare());
        buf.writeBoolean(p.animal.sick());
        buf.writeBoolean(p.animal.severe());
        buf.writeBoolean(p.animal.baby());
        buf.writeUtf(p.animal.trait());
        buf.writeInt(p.animal.space());
        buf.writeInt(p.animal.habitat());
        buf.writeInt(p.animal.food());
        buf.writeInt(p.animal.company());
        buf.writeInt(p.animal.health());
        buf.writeUtf(p.animal.customName());
        buf.writeInt(p.animal.captureDay());
        buf.writeInt(p.animal.healCount());
        buf.writeInt(p.animal.babyCount());
        buf.writeInt(p.animal.rarity());
        buf.writeInt(p.animal.generation());
        buf.writeUtf(p.animal.bestFriendName());
    }

    public static OpenAnimalCardS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenAnimalCardS2CPacket(new SyncAnimalsS2CPacket.AnimalInfo(
                buf.readInt(), buf.readUtf(), buf.readUtf(), buf.readInt(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readUtf(),
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(),
                buf.readInt(), buf.readInt(), buf.readUtf()));
    }

    public static void handle(OpenAnimalCardS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.lex3d.ultimatezootaming.client.ClientSetup.openAnimalCard(packet.animal)));
        ctx.setPacketHandled(true);
    }
}
