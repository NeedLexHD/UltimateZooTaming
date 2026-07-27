package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.welfare.HabitatManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** Ouvre le GUI d'assignation des habitats depuis le tableau de bord (OP requis). */
public class RequestHabitatGuiC2SPacket {

    public RequestHabitatGuiC2SPacket() {}

    public static void encode(RequestHabitatGuiC2SPacket p, FriendlyByteBuf buf) {}

    public static RequestHabitatGuiC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestHabitatGuiC2SPacket();
    }

    public static void handle(RequestHabitatGuiC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !player.hasPermissions(2)) return;
            NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new OpenHabitatGuiS2CPacket(HabitatManager.all()));
        });
        ctx.setPacketHandled(true);
    }
}
