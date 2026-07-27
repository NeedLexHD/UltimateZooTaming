package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Ouvre (ou met a jour) l'ecran de la Caisse du Zoo. */
public class OpenVaultS2CPacket {

    public final BlockPos pos;
    public final int balance;
    public final int total;

    public OpenVaultS2CPacket(BlockPos pos, int balance, int total) {
        this.pos = pos;
        this.balance = balance;
        this.total = total;
    }

    public static void encode(OpenVaultS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.balance);
        buf.writeInt(p.total);
    }

    public static OpenVaultS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenVaultS2CPacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    public static void handle(OpenVaultS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.lex3d.ultimatezootaming.client.ClientSetup.openVault(packet)));
        ctx.setPacketHandled(true);
    }
}
