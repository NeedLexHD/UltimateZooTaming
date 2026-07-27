package com.lex3d.ultimatezootaming.saveddata;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/** Le DORTOIR du personnel : une parcelle rectangulaire (2 coins) ou les
 *  employes vont dormir la nuit. Delimitee par le Selecteur de parcelle. */
public class ZooDormitory extends SavedData {

    private static final String NAME = "ultimatezootame_dormitory";

    @Nullable private BlockPos cornerA, cornerB;

    public static ZooDormitory get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ZooDormitory::load, ZooDormitory::new, NAME);
    }

    public boolean isDefined() { return cornerA != null && cornerB != null; }

    public void setCorners(BlockPos a, BlockPos b) {
        this.cornerA = a;
        this.cornerB = b;
        setDirty();
    }

    @Nullable
    public AABB bounds() {
        if (!isDefined()) return null;
        return new AABB(
                Math.min(cornerA.getX(), cornerB.getX()), Math.min(cornerA.getY(), cornerB.getY()),
                Math.min(cornerA.getZ(), cornerB.getZ()),
                Math.max(cornerA.getX(), cornerB.getX()) + 1, Math.max(cornerA.getY(), cornerB.getY()) + 1,
                Math.max(cornerA.getZ(), cornerB.getZ()) + 1);
    }

    /** Centre approximatif de la parcelle (repli si un employe ne trouve pas de lit). */
    @Nullable
    public BlockPos center() {
        if (!isDefined()) return null;
        return BlockPos.containing(bounds().getCenter());
    }

    public boolean contains(BlockPos pos) {
        AABB b = bounds();
        return b != null && b.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (cornerA != null) tag.putLong("A", cornerA.asLong());
        if (cornerB != null) tag.putLong("B", cornerB.asLong());
        return tag;
    }

    public static ZooDormitory load(CompoundTag tag) {
        ZooDormitory d = new ZooDormitory();
        if (tag.contains("A")) d.cornerA = BlockPos.of(tag.getLong("A"));
        if (tag.contains("B")) d.cornerB = BlockPos.of(tag.getLong("B"));
        return d;
    }
}
