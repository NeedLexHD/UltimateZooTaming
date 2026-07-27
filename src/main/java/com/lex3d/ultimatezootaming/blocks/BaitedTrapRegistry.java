package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.items.KibbleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registre transitoire des pieges appates, AVEC LE REGIME de l'appat.
 *
 * Regle : un mob n'est ATTIRE que par une croquette de SON regime.
 * - Croquette Carnivore -> attire uniquement les carnivores, etc.
 * - Appat quelconque (pas une croquette) -> n'attire PERSONNE (Optional.empty) ;
 *   la capture au contact reste possible si un mob marche dessus par hasard.
 *
 * Optional car ConcurrentHashMap n'accepte pas de valeur null.
 */
public class BaitedTrapRegistry {

    /** Regime de l'appat (empty = appat generique, n'attire pas) + taille max de mob acceptee. */
    public record TrapInfo(Optional<KibbleItem.Diet> diet, float maxMobSize) {}

    private static final Map<ResourceKey<Level>, Map<BlockPos, TrapInfo>> BAITED_TRAPS = new HashMap<>();

    public static void register(Level level, BlockPos pos, @Nullable KibbleItem.Diet diet, float maxMobSize) {
        if (level.isClientSide()) return;
        BAITED_TRAPS.computeIfAbsent(level.dimension(), k -> new ConcurrentHashMap<>())
                .put(pos.immutable(), new TrapInfo(Optional.ofNullable(diet), maxMobSize));
    }

    /** Le piege a cette position est-il toujours appate ? (pour ZooAttractionGoal) */
    public static boolean isStillActive(Level level, BlockPos pos) {
        Map<BlockPos, TrapInfo> map = BAITED_TRAPS.get(level.dimension());
        return map != null && map.containsKey(pos);
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        Map<BlockPos, TrapInfo> map = BAITED_TRAPS.get(level.dimension());
        if (map != null) map.remove(pos);
    }

    /**
     * Piege appate le plus proche DONT L'APPAT CORRESPOND AU REGIME de ce mob
     * ET dont la taille accepte ce mob, ou null si aucun. Un piege avec un appat
     * non-croquette n'attire jamais ; un piege trop petit n'attire pas non plus
     * (sinon le mob resterait plante devant sans jamais declencher).
     */
    @Nullable
    public static BlockPos findNearestMatching(Level level, BlockPos origin, double maxDist, LivingEntity mob) {
        Map<BlockPos, TrapInfo> map = BAITED_TRAPS.get(level.dimension());
        if (map == null || map.isEmpty()) return null;

        float mobSize = Math.max(mob.getBbWidth(), mob.getBbHeight());

        BlockPos best = null;
        double bestDistSq = maxDist * maxDist;
        for (Map.Entry<BlockPos, TrapInfo> entry : map.entrySet()) {
            TrapInfo info = entry.getValue();
            if (info.diet().isEmpty() || !info.diet().get().matches(mob)) continue;
            if (mobSize > info.maxMobSize()) continue;

            double d = origin.distSqr(entry.getKey());
            if (d < bestDistSq) {
                bestDistSq = d;
                best = entry.getKey();
            }
        }
        return best;
    }
}
