package com.lex3d.ultimatezootaming.saveddata;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Totalement passif : on ne fait AUCUN calcul lourd ici, uniquement de la lecture/
 * ecriture pour peupler la liste du GUI Sifflet (WhistleScreen) a la demande.
 */
public class ZooSavedData extends SavedData {

    private static final String NAME = UltimateZooTame.MODID + "_zoo_data";

    private final Map<UUID, Set<UUID>> ownerToFamiliars = new HashMap<>();
    private final Map<UUID, ZooZone> zones = new HashMap<>();

    public static ZooSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ZooSavedData::load, ZooSavedData::new, NAME);
    }

    public void addFamiliar(UUID owner, UUID familiar) {
        ownerToFamiliars.computeIfAbsent(owner, k -> new HashSet<>()).add(familiar);
        setDirty();
    }

    public void removeFamiliar(UUID owner, UUID familiar) {
        Set<UUID> set = ownerToFamiliars.get(owner);
        if (set != null) {
            set.remove(familiar);
            setDirty();
        }
    }

    public Set<UUID> getFamiliars(UUID owner) {
        return ownerToFamiliars.getOrDefault(owner, Collections.emptySet());
    }

    // ---- Zones d'enclos (formes libres, voir zones/ZooZone) ----

    /** Force la sauvegarde (ex: apres renommage d'une zone). */
    public void markChanged() {
        setDirty();
    }

    public void addZone(ZooZone zone) {
        zones.put(zone.getId(), zone);
        setDirty();
    }

    public void removeZone(UUID zoneId) {
        zones.remove(zoneId);
        setDirty();
    }

    public ZooZone getZone(UUID zoneId) {
        return zoneId == null ? null : zones.get(zoneId);
    }

    /** Toutes les zones, tous proprietaires confondus. */
    public Collection<ZooZone> getAllZones() {
        return zones.values();
    }

    public Collection<ZooZone> getZones(UUID owner) {
        List<ZooZone> result = new ArrayList<>();
        for (ZooZone zone : zones.values()) {
            if (zone.getOwnerUUID().equals(owner)) result.add(zone);
        }
        return result;
    }

    /** La zone (du proprietaire donne) contenant cette position, ou null. */
    public ZooZone getZoneAt(UUID owner, net.minecraft.core.BlockPos pos) {
        for (ZooZone zone : zones.values()) {
            if (zone.getOwnerUUID().equals(owner) && zone.contains(pos)) return zone;
        }
        return null;
    }

    public int countZones() {
        return zones.size();
    }

    private static ZooSavedData load(CompoundTag tag) {
        ZooSavedData data = new ZooSavedData();
        ListTag ownersList = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < ownersList.size(); i++) {
            CompoundTag entry = ownersList.getCompound(i);
            UUID owner = entry.getUUID("Owner");
            Set<UUID> familiars = new HashSet<>();
            ListTag familiarsList = entry.getList("Familiars", Tag.TAG_INT_ARRAY);
            for (int j = 0; j < familiarsList.size(); j++) {
                familiars.add(NbtUtils.loadUUID(familiarsList.get(j)));
            }
            data.ownerToFamiliars.put(owner, familiars);
        }
        ListTag zonesList = tag.getList("Zones", Tag.TAG_COMPOUND);
        for (int i = 0; i < zonesList.size(); i++) {
            ZooZone zone = ZooZone.load(zonesList.getCompound(i));
            data.zones.put(zone.getId(), zone);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag ownersList = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> entry : ownerToFamiliars.entrySet()) {
            CompoundTag ownerTag = new CompoundTag();
            ownerTag.putUUID("Owner", entry.getKey());
            ListTag familiarsList = new ListTag();
            for (UUID familiar : entry.getValue()) {
                familiarsList.add(NbtUtils.createUUID(familiar));
            }
            ownerTag.put("Familiars", familiarsList);
            ownersList.add(ownerTag);
        }
        tag.put("Owners", ownersList);
        ListTag zonesList = new ListTag();
        for (ZooZone zone : zones.values()) {
            zonesList.add(zone.save());
        }
        tag.put("Zones", zonesList);
        return tag;
    }
}
