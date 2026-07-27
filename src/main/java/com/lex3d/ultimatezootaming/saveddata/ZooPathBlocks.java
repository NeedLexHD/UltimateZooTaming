package com.lex3d.ultimatezootaming.saveddata;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/** Types de blocs marques comme ALLEE en jeu (via le clic sur la carte / config). */
public class ZooPathBlocks extends SavedData {

    private static final String NAME = "ultimatezootame_pathblocks";
    private final Set<String> ids = new HashSet<>();

    public static ZooPathBlocks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                ZooPathBlocks::load, ZooPathBlocks::new, NAME);
    }

    public boolean contains(BlockState state) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return id != null && ids.contains(id.toString());
    }

    public boolean toggle(Block block) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
        if (id == null) return false;
        String s = id.toString();
        boolean added;
        if (ids.contains(s)) { ids.remove(s); added = false; }
        else { ids.add(s); added = true; }
        setDirty();
        return added;
    }

    public java.util.List<String> list() { return new java.util.ArrayList<>(ids); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag l = new ListTag();
        for (String s : ids) l.add(StringTag.valueOf(s));
        tag.put("Ids", l);
        return tag;
    }

    public static ZooPathBlocks load(CompoundTag tag) {
        ZooPathBlocks p = new ZooPathBlocks();
        ListTag l = tag.getList("Ids", 8);
        for (int i = 0; i < l.size(); i++) p.ids.add(l.getString(i));
        return p;
    }
}
