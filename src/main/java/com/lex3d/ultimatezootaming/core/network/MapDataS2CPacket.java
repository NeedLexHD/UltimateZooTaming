package com.lex3d.ultimatezootaming.core.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * La Carte du Zoo : fenetre de 64x64 blocs. colors = couleur de carte du bloc
 * de surface, flags par case : bit0 = allee de zoo, bit1 = chunk revendique.
 */
public class MapDataS2CPacket {

    public static final int SIZE = 64;

    /** Marqueur : 0 entree, 1 caisse du zoo, 2 caisse enregistreuse, 3 visiteur,
     *  4 employe, 5 animal ECHAPPE, 6 joueur. */
    public record Marker(int x, int z, int type) {}

    public final int centerX, centerZ;
    public final byte[] colors;   // SIZE*SIZE ids de MapColor
    public final byte[] flags;    // SIZE*SIZE bits (MODIFIABLE cote client : edition optimiste)
    public final byte[] heights;  // SIZE*SIZE hauteurs (pour l'ombrage du relief)
    public final List<Marker> markers;
    public final int claimedCount, maxChunks, zooScore;

    public MapDataS2CPacket(int centerX, int centerZ, byte[] colors, byte[] flags, byte[] heights,
                            List<Marker> markers, int claimedCount, int maxChunks, int zooScore) {
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.colors = colors;
        this.flags = flags;
        this.heights = heights;
        this.markers = markers;
        this.claimedCount = claimedCount;
        this.maxChunks = maxChunks;
        this.zooScore = zooScore;
    }

    public static void encode(MapDataS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.centerX);
        buf.writeInt(p.centerZ);
        buf.writeByteArray(p.colors);
        buf.writeByteArray(p.flags);
        buf.writeByteArray(p.heights);
        buf.writeInt(p.markers.size());
        for (Marker m : p.markers) {
            buf.writeInt(m.x()); buf.writeInt(m.z()); buf.writeByte(m.type());
        }
        buf.writeInt(p.claimedCount);
        buf.writeInt(p.maxChunks);
        buf.writeInt(p.zooScore);
    }

    public static MapDataS2CPacket decode(FriendlyByteBuf buf) {
        int cx = buf.readInt(), cz = buf.readInt();
        byte[] colors = buf.readByteArray();
        byte[] flags = buf.readByteArray();
        byte[] heights = buf.readByteArray();
        int n = buf.readInt();
        List<Marker> markers = new ArrayList<>();
        for (int i = 0; i < n; i++) markers.add(new Marker(buf.readInt(), buf.readInt(), buf.readByte()));
        return new MapDataS2CPacket(cx, cz, colors, flags, heights, markers,
                buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(MapDataS2CPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                        com.lex3d.ultimatezootaming.client.ClientSetup.openOrUpdateMap(packet)));
        ctx.setPacketHandled(true);
    }
}
