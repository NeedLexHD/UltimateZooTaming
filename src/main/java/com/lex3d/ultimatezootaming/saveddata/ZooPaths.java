package com.lex3d.ultimatezootaming.saveddata;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Les CHEMINS du zoo : des cases (x,z) MARQUEES sur la Carte par le directeur,
 * par-dessus les allees qu'il a construites lui-meme (slabs, planches...).
 * Aucun bloc n'est pose : c'est un plan de circulation, et les visiteurs
 * SUIVENT ces cases pour se deplacer.
 */
public class ZooPaths extends SavedData {

    private static final String NAME = "ultimatezootame_paths";
    private final Set<Long> cells = new HashSet<>();

    public static ZooPaths get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ZooPaths::load, ZooPaths::new, NAME);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public boolean isMarked(int x, int z) { return cells.contains(key(x, z)); }

    public boolean isEmpty() { return cells.isEmpty(); }

    public void mark(int x, int z) { if (cells.add(key(x, z))) setDirty(); }

    public void unmark(int x, int z) { if (cells.remove(key(x, z))) setDirty(); }

    /** La case marquee la plus proche de (x,z), a range max, ou null. */
    public BlockPos nearest(int x, int z, int range) {
        long best = Long.MAX_VALUE;
        int bx = 0, bz = 0;
        for (long k : cells) {
            int cx = (int) (k >> 32), cz = (int) k;
            long d = (long) (cx - x) * (cx - x) + (long) (cz - z) * (cz - z);
            if (d < best) { best = d; bx = cx; bz = cz; }
        }
        return best <= (long) range * range ? new BlockPos(bx, 0, bz) : null;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] arr = new long[cells.size()];
        int i = 0;
        for (long l : cells) arr[i++] = l;
        tag.putLongArray("Cells", arr);
        return tag;
    }

    public static ZooPaths load(CompoundTag tag) {
        ZooPaths p = new ZooPaths();
        for (long l : tag.getLongArray("Cells")) p.cells.add(l);
        return p;
    }
}
