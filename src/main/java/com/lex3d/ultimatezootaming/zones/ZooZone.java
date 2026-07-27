package com.lex3d.ultimatezootaming.zones;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Un enclos de forme LIBRE : pas un rectangle, mais l'ensemble exact des colonnes
 * de sol (x, y, z du bloc marchable) detectees par EnclosureScanner a l'interieur
 * des barrieres (clotures/murs/denivelles). Epouse n'importe quelle forme.
 */
public class ZooZone {

    private final UUID id;
    private String name;
    private final UUID ownerUUID;
    /** Colonnes du sol de l'enclos, en BlockPos.asLong (la position du bloc SUR lequel on marche). */
    private final Set<Long> floorColumns;

    // Boite englobante (acceleration des tests de proximite uniquement)
    private int minX, minY, minZ, maxX, maxY, maxZ;
    /** 0 = Enclos (animaux), 1 = Repos employes, 2 = Vente, 3 = Stockage. */
    private int zoneType = 0;

    public ZooZone(UUID id, String name, UUID ownerUUID, Set<Long> floorColumns) {
        this.id = id;
        this.name = name;
        this.ownerUUID = ownerUUID;
        this.floorColumns = floorColumns;
        recomputeBounds();
    }

    private void recomputeBounds() {
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
        for (long packed : floorColumns) {
            BlockPos pos = BlockPos.of(packed);
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
        }
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public int size() { return floorColumns.size(); }
    public Set<Long> floorColumns() { return floorColumns; }

    /** Comme contains, mais tolere 1 colonne de marge (blocs poses au bord/sur le mur). */
    public boolean containsNear(BlockPos pos) {
        if (contains(pos)) return true;
        // Tolerance de 4 blocs autour de l'enclos : le panneau se pose A COTE
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (contains(pos.offset(dx, 0, dz))) return true;
            }
        }
        return false;
    }

    /** Hauteur du VOLUME de l'enclos au-dessus du sol scanne (oiseaux, eau, arbres). */
    public static final int VOLUME_HEIGHT = 40;

    /** Ce (x, y, z) fait-il partie de l'enclos ? La zone est un VOLUME : toute la
     *  colonne du sol scanne (marge -4 pour l'eau/creux) jusqu'a +40 blocs au-dessus. */
    public boolean contains(BlockPos pos) {
        if (pos.getX() < minX || pos.getX() > maxX || pos.getZ() < minZ || pos.getZ() > maxZ) return false;
        for (int dy = -4; dy <= VOLUME_HEIGHT; dy++) {
            if (floorColumns.contains(BlockPos.asLong(pos.getX(), pos.getY() - dy - 1, pos.getZ()))) {
                return true;
            }
        }
        return false;
    }

    /** Une colonne de sol aleatoire de l'enclos (pour l'errance). */
    public BlockPos randomFloorPos(java.util.Random random) {
        int target = random.nextInt(floorColumns.size());
        int i = 0;
        for (long packed : floorColumns) {
            if (i++ == target) return BlockPos.of(packed);
        }
        return BlockPos.of(floorColumns.iterator().next());
    }

    /** La colonne de sol la plus proche de pos (retour au bercail). */
    public BlockPos nearestFloorPos(BlockPos from) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (long packed : floorColumns) {
            BlockPos pos = BlockPos.of(packed);
            double d = pos.distSqr(from);
            if (d < bestDist) {
                bestDist = d;
                best = pos;
            }
        }
        return best;
    }

    /** Colonnes de bordure (au moins un voisin hors zone) : pour la visualisation en particules. */
    /** Boite englobante de l'enclos (pour les requetes d'entites larges). */
    public net.minecraft.world.phys.AABB boundingBox() {
        return new net.minecraft.world.phys.AABB(minX, minY - 4, minZ,
                maxX + 1, maxY + VOLUME_HEIGHT, maxZ + 1);
    }

    /** Acces brut aux colonnes de sol (pour le scan d'habitat). */
    public Set<Long> floorColumnsRaw() {
        return floorColumns;
    }

    public Set<BlockPos> borderColumns() {
        Set<BlockPos> border = new HashSet<>();
        for (long packed : floorColumns) {
            BlockPos pos = BlockPos.of(packed);
            boolean edge = false;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                boolean neighborInside = false;
                for (int dy = -1; dy <= 1; dy++) {
                    if (floorColumns.contains(BlockPos.asLong(pos.getX() + d[0], pos.getY() + dy, pos.getZ() + d[1]))) {
                        neighborInside = true;
                        break;
                    }
                }
                if (!neighborInside) { edge = true; break; }
            }
            if (edge) border.add(pos);
        }
        return border;
    }

    public int getZoneType() { return zoneType; }

    public void setZoneType(int type) { this.zoneType = Math.max(0, Math.min(3, type)); }

    /** Une zone d'animaux (le seul type compte pour le bien-etre et la note). */
    public boolean isAnimalZone() { return zoneType == 0; }

    public static String typeKey(int type) {
        return switch (type) {
            case 1 -> "rest";
            case 2 -> "sale";
            case 3 -> "storage";
            default -> "animal";
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putUUID("Owner", ownerUUID);
        long[] arr = new long[floorColumns.size()];
        int i = 0;
        for (long packed : floorColumns) arr[i++] = packed;
        tag.putLongArray("Floor", arr);
        tag.putInt("ZoneType", zoneType);
        return tag;
    }

    public static ZooZone load(CompoundTag tag) {
        Set<Long> floor = new HashSet<>();
        for (long packed : tag.getLongArray("Floor")) floor.add(packed);
        ZooZone z = new ZooZone(tag.getUUID("Id"), tag.getString("Name"), tag.getUUID("Owner"), floor);
        z.zoneType = tag.getInt("ZoneType");
        return z;
    }
}
