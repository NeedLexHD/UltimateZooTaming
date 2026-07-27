package com.lex3d.ultimatezootaming.core.network;

import com.lex3d.ultimatezootaming.blocks.ShopBlock;
import com.lex3d.ultimatezootaming.blocks.ZooEntranceBlock;
import com.lex3d.ultimatezootaming.blocks.ZooPathBlock;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooTerritory;
import com.lex3d.ultimatezootaming.welfare.ZooScore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Le client demande la Carte du Zoo centree quelque part (ouverture ou pan). */
public class RequestMapC2SPacket {

    private final int centerX, centerZ;

    public RequestMapC2SPacket(int centerX, int centerZ) {
        this.centerX = centerX;
        this.centerZ = centerZ;
    }

    public static void encode(RequestMapC2SPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.centerX);
        buf.writeInt(p.centerZ);
    }

    public static RequestMapC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestMapC2SPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(RequestMapC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            sendMapTo(player, packet.centerX, packet.centerZ);
        });
        ctx.setPacketHandled(true);
    }

    /** Construit et envoie la fenetre de carte 64x64 autour de (cx, cz). */
    public static void sendMapTo(ServerPlayer player, int cx, int cz) {
        ServerLevel level = player.serverLevel();
        int size = MapDataS2CPacket.SIZE;
        int x0 = cx - size / 2, z0 = cz - size / 2;
        byte[] colors = new byte[size * size];
        byte[] flags = new byte[size * size];
        byte[] heights = new byte[size * size];
        ZooTerritory territory = ZooTerritory.get(level);
        var zooPaths = com.lex3d.ultimatezootaming.saveddata.ZooPaths.get(level);

        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int idx = dz * size + dx;
                if (!level.isLoaded(new BlockPos(x, 64, z))) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                var state = level.getBlockState(p);
                colors[idx] = (byte) state.getMapColor(level, p).id;
                heights[idx] = (byte) Math.max(0, Math.min(255, y + 64));
                int f = 0;
                if (zooPaths.isMarked(x, z)) f |= 1;
                if (territory.isClaimed(x, z)) f |= 2;
                flags[idx] = (byte) f;
            }
        }

        List<MapDataS2CPacket.Marker> markers = new ArrayList<>();
        AABB window = new AABB(x0, level.getMinBuildHeight(), z0,
                x0 + size, level.getMaxBuildHeight(), z0 + size);
        for (BlockPos e : ZooEntranceBlock.entrancesIn(level)) {
            if (window.contains(e.getX(), 64, e.getZ())) markers.add(marker(e, 0, x0, z0));
        }
        for (BlockPos s : ShopBlock.allShops(level)) {
            if (window.contains(s.getX(), 64, s.getZ())) markers.add(marker(s, 2, x0, z0));
        }
        for (VisitorEntity v : level.getEntitiesOfClass(VisitorEntity.class, window)) {
            markers.add(new MapDataS2CPacket.Marker((int) v.getX() - x0, (int) v.getZ() - z0, 3));
        }
        for (ZooKeeperEntity k : level.getEntitiesOfClass(ZooKeeperEntity.class, window)) {
            markers.add(new MapDataS2CPacket.Marker((int) k.getX() - x0, (int) k.getZ() - z0, 4));
        }
        for (Animal a : level.getEntitiesOfClass(Animal.class, window,
                an -> an.getCapability(CapabilityHandler.TAMING_DATA)
                        .resolve().map(d -> d.isEscaped()).orElse(false))) {
            markers.add(new MapDataS2CPacket.Marker((int) a.getX() - x0, (int) a.getZ() - z0, 5));
        }
        markers.add(new MapDataS2CPacket.Marker(
                (int) player.getX() - x0, (int) player.getZ() - z0, 6));

        int score = ZooScore.compute(level);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MapDataS2CPacket(cx, cz, colors, flags, heights, markers,
                        territory.count(), 0, score));
    }

    private static MapDataS2CPacket.Marker marker(BlockPos p, int type, int x0, int z0) {
        return new MapDataS2CPacket.Marker(p.getX() - x0, p.getZ() - z0, type);
    }
}
