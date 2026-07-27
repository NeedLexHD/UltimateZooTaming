package com.lex3d.ultimatezootaming.welfare;

import com.lex3d.ultimatezootaming.blocks.FeederBlockEntity;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calcule la satisfaction (0-100) d'un familier dans son enclos, SANS jamais
 * hardcoder d'espece : chaque critere lit ce que le mob DECLARE de lui-meme via
 * les API standard (WaterAnimal, MobType, fireImmune...). Un mob de n'importe
 * quel mod tombe donc toujours sur un score raisonnable, jamais bloque a 0.
 *
 * Criteres (somme = 100) :
 *   Espace 30 | Habitat 25 | Nourriture 20 | Compagnie 15 | Sante 10
 */
public class WelfareCalculator {

    // Cache d'habitat par zone (scan couteux fait 1x/minute, partage entre animaux).
    // ConcurrentHashMap : plusieurs threads (tick + capability events) lisent/ecrivent
    // simultanement ; HashMap ordinaire se corrompt silencieusement dans ce cas.
    private record HabitatCache(long computedAt, boolean hasWater, boolean hasVegetation, boolean hasLava) {}
    private static final Map<UUID, HabitatCache> HABITAT_CACHE = new ConcurrentHashMap<>();
    private static final long HABITAT_TTL = 1200; // 60s

    public static int compute(ServerLevel level, LivingEntity mob, ZooZone zone) {
        Breakdown b = computeBreakdown(level, mob, zone);
        return b.total();
    }

    /** Detail par critere (pour le diagnostic du Soigneur). Max : 30/25/20/15/10. */
    public record Breakdown(int space, int habitat, int food, int company, int health) {
        public int total() { return space + habitat + food + company + health; }
    }

    public static Breakdown computeBreakdown(ServerLevel level, LivingEntity mob, ZooZone zone) {
        int companyRaw = companyScore(level, mob, zone);
        // Trait : ajuste certains criteres (Sociable amplifie la compagnie, etc.)
        com.lex3d.ultimatezootaming.capability.TamingData.Trait trait =
                mob.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                        .resolve().map(com.lex3d.ultimatezootaming.capability.TamingData::getTrait)
                        .orElse(com.lex3d.ultimatezootaming.capability.TamingData.Trait.NONE);

        int space = spaceScore(level, mob, zone);
        int habitat = habitatScore(level, mob, zone);
        // ENRICHISSEMENT : les jouets poses dans l'enclos remontent l'habitat.
        int toys = com.lex3d.ultimatezootaming.blocks.EnrichmentBlock.countInZone(level, zone);
        if (toys > 0) habitat = Math.min(25, habitat + toys * 3);
        int food = foodScore(level, mob, zone);
        int company = companyRaw;
        int health = healthScore(mob);

        health = Math.max(0, Math.min(10, health + trait.getWelfareModifier()));

        return new Breakdown(space, habitat, food, company, health);
    }

    /** Expose l'habitat d'une zone (pour formuler les conseils du diagnostic). */
    public static boolean[] habitatOf(ServerLevel level, ZooZone zone) {
        HabitatCache h = getHabitat(level, zone);
        return new boolean[]{h.hasWater(), h.hasVegetation(), h.hasLava()};
    }

    /** ESPACE (0-30) : cases de l'enclos par animal present. */
    private static int spaceScore(ServerLevel level, LivingEntity mob, ZooZone zone) {
        int animals = countAnimalsInZone(level, zone);
        if (animals <= 0) animals = 1;
        double perAnimal = (double) zone.size() / animals;
        double ratio = Math.min(1.0, (perAnimal - 2) / 10.0);
        return (int) Math.round(Math.max(0, ratio) * 30);
    }

    /** HABITAT (0-25) : les blocs de l'enclos correspondent-ils au PROFIL du mob ? */
    private static int habitatScore(ServerLevel level, LivingEntity mob, ZooZone zone) {
        HabitatProfile profile = HabitatManager.profileOf(mob);

        if (profile == HabitatProfile.AUTO) {
            HabitatCache cache = getHabitat(level, zone);
            boolean isAquatic = mob instanceof WaterAnimal;
            boolean isFireImmune = mob.fireImmune();
            boolean isUndead = mob.getMobType() == MobType.UNDEAD;
            if (isAquatic) return cache.hasWater() ? 25 : 3;
            if (isFireImmune) return cache.hasLava() ? 25 : 12;
            if (isUndead) return 18;
            return cache.hasVegetation() ? 25 : 10;
        }

        double ratio = profileMatchRatio(level, zone, profile);
        if (ratio >= 0.25) return 25;
        if (ratio >= 0.10) return 15;
        if (ratio > 0) return 8;
        return 3;
    }

    private static double profileMatchRatio(ServerLevel level, ZooZone zone, HabitatProfile profile) {
        int step = Math.max(1, zone.size() / 128);
        int i = 0, sampled = 0, matched = 0;
        for (long packed : zone.floorColumnsRaw()) {
            if (i++ % step != 0) continue;
            sampled++;
            BlockPos floor = BlockPos.of(packed);
            for (int dy = 0; dy <= 2; dy++) {
                if (profile.matches(level.getBlockState(floor.above(dy)))
                        || (dy == 0 && profile.matches(level.getBlockState(floor)))) {
                    matched++;
                    break;
                }
            }
        }
        return sampled == 0 ? 0 : (double) matched / sampled;
    }

    /** NOURRITURE (0-20) : une Mangeoire non vide a portee du mob. */
    private static int foodScore(ServerLevel level, LivingEntity mob, ZooZone zone) {
        for (long packed : zone.floorColumns()) {
            BlockPos floor = BlockPos.of(packed);
            for (int dy = -2; dy <= 3; dy++) {
                BlockPos p = floor.above(dy);
                if (level.getBlockEntity(p) instanceof FeederBlockEntity feeder && feeder.hasFood()
                        && feeder.storedItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem fodder
                        && fodder.getDiet().matches(mob)) {
                    return 20;
                }
            }
        }
        return 0;
    }

    /**
     * COMPAGNIE (0-15) : au moins un congenere (meme type) dans l'enclos.
     *
     * Utilise zone.boundingBox() et non getBoundingBox().inflate(N) : dans un
     * grand enclos (> 24 blocs), deux individus aux coins opposes etaient hors
     * du rayon d'inflate et ne se "voyaient" jamais -> score compagnie = 0
     * meme s'ils cohabitent bien. Le filtre zone.contains() garantit qu'on
     * n'inclut pas les animaux de dehors.
     */
    private static int companyScore(ServerLevel level, LivingEntity mob, ZooZone zone) {
        List<Animal> nearby = level.getEntitiesOfClass(Animal.class,
                zone.boundingBox(),
                a -> a != mob && a.getType() == mob.getType() && zone.contains(a.blockPosition()));
        return nearby.isEmpty() ? 0 : 15;
    }

    /** SANTE (0-10) : malade ou blesse recemment fait chuter le score. */
    private static int healthScore(LivingEntity mob) {
        boolean sick = mob.getCapability(CapabilityHandler.TAMING_DATA)
                .resolve().map(com.lex3d.ultimatezootaming.capability.TamingData::isSick).orElse(false);
        if (sick) return 0;
        float hpRatio = mob.getHealth() / mob.getMaxHealth();
        return (int) Math.round(hpRatio * 10);
    }

    private static int countAnimalsInZone(ServerLevel level, ZooZone zone) {
        return level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                a -> a.isAlive() && zone.contains(a.blockPosition())
                        && a.getCapability(CapabilityHandler.TAMING_DATA)
                            .resolve().map(d -> d.isTamed()).orElse(false)).size();
    }

    private static HabitatCache getHabitat(ServerLevel level, ZooZone zone) {
        HabitatCache cached = HABITAT_CACHE.get(zone.getId());
        long now = level.getGameTime();
        if (cached != null && now - cached.computedAt() < HABITAT_TTL) {
            return cached;
        }
        boolean water = false, veg = false, lava = false;
        int step = Math.max(1, zone.size() / 256);
        int i = 0;
        for (long packed : zone.floorColumnsRaw()) {
            if (i++ % step != 0) continue;
            BlockPos floor = BlockPos.of(packed);
            for (int dy = 0; dy <= 2; dy++) {
                BlockState state = level.getBlockState(floor.above(dy));
                if (state.getFluidState().is(net.minecraft.tags.FluidTags.WATER)) water = true;
                if (state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) lava = true;
                if (state.is(BlockTags.LEAVES) || state.is(BlockTags.FLOWERS)
                        || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)
                        || state.getBlock() == net.minecraft.world.level.block.Blocks.GRASS
                        || state.getBlock() == net.minecraft.world.level.block.Blocks.TALL_GRASS
                        || state.getBlock() == net.minecraft.world.level.block.Blocks.FERN) {
                    veg = true;
                }
            }
            if (water && veg && lava) break;
        }
        HabitatCache fresh = new HabitatCache(now, water, veg, lava);
        HABITAT_CACHE.put(zone.getId(), fresh);
        return fresh;
    }

    public static void invalidate(UUID zoneId) {
        HABITAT_CACHE.remove(zoneId);
    }
}
