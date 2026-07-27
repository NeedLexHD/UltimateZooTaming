package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Le serveur pousse periodiquement l'etat "badge" des familiers proches du joueur
 * (id d'entite, index de trait, malade) pour que le client puisse dessiner une
 * icone au-dessus de chaque animal sans avoir la capability cote client.
 */
public class FamiliarBadgeS2CPacket {

    public record Badge(int entityId, int traitOrdinal, boolean sick) {}

    private final List<Badge> badges;

    public FamiliarBadgeS2CPacket(List<Badge> badges) {
        this.badges = badges;
    }

    public static void encode(FamiliarBadgeS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.badges.size());
        for (Badge b : packet.badges) {
            buf.writeInt(b.entityId());
            buf.writeByte(b.traitOrdinal());
            buf.writeBoolean(b.sick());
        }
    }

    public static FamiliarBadgeS2CPacket decode(FriendlyByteBuf buf) {
        int n = buf.readInt();
        List<Badge> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(new Badge(buf.readInt(), buf.readByte(), buf.readBoolean()));
        }
        return new FamiliarBadgeS2CPacket(list);
    }

    public static void handle(FamiliarBadgeS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.lex3d.ultimatezootaming.client.ClientBadgeCache.update(packet.badges)));
        ctx.setPacketHandled(true);
    }
}
