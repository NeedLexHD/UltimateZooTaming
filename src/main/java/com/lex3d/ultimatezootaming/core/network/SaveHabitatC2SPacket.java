package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.welfare.HabitatManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Le client (GUI admin) sauve l'habitat+regime d'un type de mob. Perm 2 requise. */
public class SaveHabitatC2SPacket {

    private final String typeId;
    private final int habitat;
    private final int diet;

    public SaveHabitatC2SPacket(String typeId, int habitat, int diet) {
        this.typeId = typeId;
        this.habitat = habitat;
        this.diet = diet;
    }

    public static void encode(SaveHabitatC2SPacket p, FriendlyByteBuf buf) {
        buf.writeUtf(p.typeId);
        buf.writeByte(p.habitat);
        buf.writeByte(p.diet);
    }

    public static SaveHabitatC2SPacket decode(FriendlyByteBuf buf) {
        return new SaveHabitatC2SPacket(buf.readUtf(), buf.readByte(), buf.readByte());
    }

    public static void handle(SaveHabitatC2SPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player == null || !player.hasPermissions(2)) return;
            HabitatManager.set(p.typeId, p.habitat, p.diet);
        });
        ctx.get().setPacketHandled(true);
    }
}
