package com.lex3d.ultimatezootaming.welfare;

import com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ambiance d'un enclos (0-10) : la deco AUTOUR compte — fleurs, feuillages,
 * lanternes, bancs, eau. Un beau zoo attire plus de monde. Cache 60s.
 */
public final class AmbianceScore {

    private record Cached(long at, int score) {}
    private static final Map<UUID, Cached> CACHE = new ConcurrentHashMap<>();
    private static final long TTL = 1200; // 60s

    private AmbianceScore() {}

    public static int of(ServerLevel level, ZooZone zone) {
        Cached c = CACHE.get(zone.getId());
        long now = level.getGameTime();
        if (c != null && now - c.at() < TTL) return c.score();
        int score = compute(level, zone);
        CACHE.put(zone.getId(), new Cached(now, score));
        return score;
    }

    /** Moyenne d'ambiance des enclos (pour l'affluence). */
    public static double zooAverage(ServerLevel level) {
        var zones = com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones();
        int sum = 0, n = 0;
        for (ZooZone z : zones) {
            if (!z.isAnimalZone()) continue;
            sum += of(level, z);
            n++;
        }
        return n == 0 ? 0 : (double) sum / n;
    }

    private static int compute(ServerLevel level, ZooZone zone) {
        AABB bb = zone.boundingBox();
        // Scanne du sol jusqu'a +8 blocs : capte aussi les ARBRES/sapins hauts.
        int y0 = (int) bb.minY - 1, y1 = (int) bb.minY + 8;
        int flowers = 0, leaves = 0, lights = 0, water = 0, litter = 0;
        int minX = (int) bb.minX - 3, maxX = (int) bb.maxX + 3;
        int minZ = (int) bb.minZ - 3, maxZ = (int) bb.maxZ + 3;
        for (int x = minX; x <= maxX; x += 2) {
            for (int z = minZ; z <= maxZ; z += 2) {
                boolean inside = x > bb.minX + 2 && x < bb.maxX - 2 && z > bb.minZ + 2 && z < bb.maxZ - 2;
                if (inside) continue; // seule la bordure compte
                for (int y = y0; y <= y1; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir()) continue;
                    if (s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)) flowers++;
                    else if (s.is(BlockTags.LEAVES)) leaves++;
                    else if (s.is(Blocks.LANTERN) || s.is(Blocks.SOUL_LANTERN)
                            || s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH)
                            || s.is(Blocks.SEA_LANTERN) || s.is(Blocks.GLOWSTONE)) lights++;
                    else if (s.is(Blocks.WATER) || s.is(Blocks.ICE) || s.is(Blocks.BLUE_ICE)
                            || s.is(Blocks.PACKED_ICE)) water++;
                    else if (s.is(com.lex3d.ultimatezootaming.core.init.ModBlocks.LITTER.get())) litter++;
                }
            }
        }

        // MALUS DETRITUS : un parc sale perd son charme (jusqu'a -4 points)
        int litterMalus = Math.min(4, litter / 2);

        int benches = 0;
        // Bancs a proximite (registre leger)
        BlockPos center = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
        if (ZooAmenityBlock.nearest(ZooAmenityBlock.Kind.BENCH, level, center, 12) != null) benches = 1;
        int raw = Math.min(3, flowers / 3) + Math.min(3, leaves / 8)
                + Math.min(2, lights / 2) + Math.min(1, water / 4) + benches
                - litterMalus;
        return Math.max(0, Math.min(10, raw));
    }
}
