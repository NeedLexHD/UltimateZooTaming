package com.lex3d.ultimatezootaming.zones;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Detection AUTOMATIQUE d'un enclos physique, quelle que soit sa forme ET quel
 * que soit le MATERIAU des murs (terre, gres, pierre, cloture, muret...).
 *
 * Principe : flood fill horizontal (BFS) sur les cases OU L'ON PEUT SE TENIR
 * DEBOUT (sol solide + 2 blocs d'air au-dessus). La propagation s'arrete
 * naturellement partout ou l'on ne peut pas marcher :
 *   - un mur (peu importe la matiere : des que c'est haut/plein, pas d'air pour
 *     les pieds -> barriere)
 *   - une cloture / un portillon / un muret (hitbox haute -> pas traversable)
 *   - un denivele de plus d'1 bloc (falaise ou fosse)
 *   - le vide / l'eau profonde
 *
 * On suit les pentes douces (+-1 bloc) comme le pathfinding des mobs, donc un
 * enclos avec un sol vallonne marche aussi.
 *
 * Si l'enclos fuit (trou dans le mur), le fill s'echappe et depasse la limite
 * de securite -> echec explicite = detecteur de fuites.
 */
public class EnclosureScanner {

    public static final int MAX_COLUMNS = 200000; // enclos enormes OK ; garde-fou anti-fuite
    public static final int MIN_COLUMNS = 8;       // mini pour les insectes

    public static Optional<Set<Long>> scan(ServerLevel level, BlockPos clickedFloor) {
        // Point de depart : la case marchable au niveau (ou juste au-dessus) du clic
        BlockPos start = findStandable(level, clickedFloor.above());
        if (start == null) start = findStandable(level, clickedFloor.above(2));
        if (start == null) {
            // Aquarium / bassin : cherche une case d'EAU valide en montant depuis le clic
            // (on a peut-etre clique le fond, une vitre, ou a travers l'eau)
            for (int dy = 0; dy <= 6 && start == null; dy++) {
                BlockPos p = clickedFloor.above(dy);
                if (level.getBlockState(p).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                    start = p;
                }
            }
            if (start == null) return Optional.empty();
        }

        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start.asLong());
        queue.add(start);

        while (!queue.isEmpty()) {
            if (visited.size() > MAX_COLUMNS) return Optional.empty(); // enclos ouvert / trop grand
            BlockPos standable = queue.poll();

            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighborColumn = standable.relative(dir);
                // MUR : si la case voisine (au niveau des pieds) est un bloc a collision
                // pleine (verre, pierre, mur d'aquarium...), c'est une barriere -> on
                // ne franchit PAS, meme sous l'eau. Corrige l'eau qui "fuyait" par le verre.
                BlockState wallState = level.getBlockState(neighborColumn);
                if (!wallState.getCollisionShape(level, neighborColumn).isEmpty()
                        && wallState.getFluidState().isEmpty()) {
                    continue; // bloc solide non-liquide = mur infranchissable
                }
                // On teste la case voisine au meme niveau, puis +1 (monter) puis -1 (descendre)
                BlockPos neighbor = findStandableNear(level, neighborColumn, standable.getY());
                if (neighbor == null) continue; // mur / cloture / falaise : barriere naturelle

                if (visited.add(neighbor.asLong())) {
                    queue.add(neighbor);
                }
            }
        }
        // On stocke la position du SOL (une case sous les pieds) pour ZooZone.
        // Pour les cases d'EAU, on descend jusqu'au VRAI FOND du bassin et on
        // enregistre toute la colonne immergee : le bassin est pris en compte
        // jusqu'au fond, pas seulement a la hauteur ou le scan a nage.
        Set<Long> floor = new HashSet<>();
        for (long packed : visited) {
            BlockPos feet = BlockPos.of(packed);
            BlockState feetState = level.getBlockState(feet);
            if (feetState.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                // Descendre tant que c'est de l'eau (max 40 blocs de profondeur)
                BlockPos p = feet;
                int depth = 0;
                while (depth < 40
                        && level.getBlockState(p.below()).getFluidState().is(net.minecraft.tags.FluidTags.WATER)) {
                    p = p.below();
                    depth++;
                }
                // p = derniere case d'eau ; son sol = p.below() (le fond solide)
                floor.add(p.below().asLong());
            } else {
                floor.add(feet.below().asLong());
            }
        }
        if (floor.size() < MIN_COLUMNS) return Optional.empty(); // trop petit (mini 8 pour insectes)
        return Optional.of(floor);
    }

    /**
     * Peut-on se tenir debout ici ? (pieds = feet, sol = feet.below()).
     * Sol solide au-dessus duquel il y a 2 blocs traversables (air).
     */
    private static BlockPos findStandable(ServerLevel level, BlockPos feet) {
        BlockPos floor = feet.below();
        BlockState floorState = level.getBlockState(floor);
        BlockState feetState = level.getBlockState(feet);
        // Cas AQUATIQUE : la case des pieds est de l'eau -> enclos d'eau (poissons, loutres)
        boolean water = feetState.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
        boolean solidFloor = floorState.isFaceSturdy(level, floor, Direction.UP)
                || floorState.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
        if (!solidFloor && !water) return null;
        // Terrestre : besoin de 2 cases libres au-dessus ; aquatique : de l'eau suffit
        if (!water && (!isWalkThrough(level, feet) || !isWalkThrough(level, feet.above()))) return null;
        return feet;
    }

    /**
     * Cherche une case ou se tenir dans la colonne voisine, en tolerant une marche
     * de +-1 bloc (comme un mob). Renvoie la position des pieds, ou null si bloque.
     */
    private static BlockPos findStandableNear(ServerLevel level, BlockPos column, int refY) {
        for (int dy : new int[]{0, 1, -1}) {
            BlockPos feet = new BlockPos(column.getX(), refY + dy, column.getZ());
            BlockPos res = findStandable(level, feet);
            if (res != null) return res;
        }
        return null;
    }

    /** Traversable a pied : pas de collision ET pas une barriere basse (cloture/muret). */
    private static boolean isWalkThrough(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WALLS)) {
            return false; // hitbox haute : infranchissable meme si techniquement "non plein"
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }
}
