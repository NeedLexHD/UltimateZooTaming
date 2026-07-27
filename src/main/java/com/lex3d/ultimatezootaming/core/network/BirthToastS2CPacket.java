package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class BirthToastS2CPacket {

    private final UUID babyUUID;
    private final String babyDescriptionId;

    public BirthToastS2CPacket(UUID babyUUID, String babyDescriptionId) {
        this.babyUUID = babyUUID;
        this.babyDescriptionId = babyDescriptionId;
    }

    public static void encode(BirthToastS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.babyUUID);
        buf.writeUtf(packet.babyDescriptionId);
    }

    public static BirthToastS2CPacket decode(FriendlyByteBuf buf) {
        return new BirthToastS2CPacket(buf.readUUID(), buf.readUtf());
    }

    public static void handle(BirthToastS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientSetup.showBirthToast(packet.babyUUID, packet.babyDescriptionId)));
        ctx.setPacketHandled(true);
    }
}
