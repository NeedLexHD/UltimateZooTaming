package com.lex3d.ultimatezootaming.saveddata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Le TERRITOIRE du zoo : les chunks revendiques sur la Carte. La limite de
 * chunks grandit avec le rang du zoo — agrandir son parc EST la progression.
 */
public class ZooTerritory extends SavedData {

    private static final String NAME = "ultimatezootame_territory";

    private final Set<Long> chunks = new HashSet<>();

    public static ZooTerritory get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ZooTerritory::load, ZooTerritory::new, NAME);
    }

    public boolean isClaimed(ChunkPos pos) { return chunks.contains(pos.toLong()); }

    public boolean isClaimed(int blockX, int blockZ) {
        return chunks.contains(ChunkPos.asLong(blockX >> 4, blockZ >> 4));
    }

    public int count() { return chunks.size(); }

    /** Les chunks revendiques, en cle ChunkPos.toLong() (pour le forceload). */
    public java.util.Set<Long> chunkKeys() { return chunks; }

    public boolean isEmpty() { return chunks.isEmpty(); }

    /** Revendique un chunk — le territoire du zoo est ILLIMITE. */
    public void claim(ChunkPos pos) {
        if (chunks.add(pos.toLong())) setDirty();
    }

    public void unclaim(ChunkPos pos) {
        if (chunks.remove(pos.toLong())) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        long[] arr = new long[chunks.size()];
        int i = 0;
        for (long l : chunks) arr[i++] = l;
        tag.putLongArray("Chunks", arr);
        return tag;
    }

    public static ZooTerritory load(CompoundTag tag) {
        ZooTerritory t = new ZooTerritory();
        for (long l : tag.getLongArray("Chunks")) t.chunks.add(l);
        return t;
    }
}
