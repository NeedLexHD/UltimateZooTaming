package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Sans enclos assigne, le soigneur retourne au Vestiaire le plus proche (jusqu'a
 * 64 blocs) et flane autour (rayon ~5 blocs) au lieu d'errer dans la nature.
 */
public class KeeperRestGoal extends Goal {

    private static final double SEARCH_RANGE = 128.0;
    private static final int HOME_RADIUS = 10;

    private final ZooKeeperEntity keeper;
    private BlockPos locker;
    private com.lex3d.ultimatezootaming.zones.ZooZone restZone;
    private int cooldown;

    public KeeperRestGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) return false;
        cooldown = 100;
        // Le zoo est OUVERT (jour) : les soigneurs travaillent, pas de pause au
        // vestiaire. Le repos ne se fait que zoo FERME (la nuit), et le sommeil au
        // dortoir est gere par KeeperSleepGoal.
        if (keeper.level() instanceof net.minecraft.server.level.ServerLevel sl
                && com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(sl).isOpen()) {
            return false;
        }
        int job = keeper.getJob();
        if (job == 3) return false; // Gardien : toujours en patrouille, jamais en pause
        if (job == 4 && !keeper.isOnStrike()) return false; // Vendeur : il tient sa boutique
        // Assigne a un enclos : pause SEULEMENT si l'enclos n'a besoin de rien
        if (job == 0 && keeper.getAssignedZone() != null && hasWorkInZone()) return false;
        // Vet/Nourrisseur : la pause s'interrompt d'elle-meme, leurs goals (prio 1-2) reprennent la main
        // Reference : le centre de SON enclos si assigne (regroupe les soigneurs par secteur)
        BlockPos ref = keeper.blockPosition();
        if (keeper.getAssignedZone() != null
                && keeper.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            var z = com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(sl).getZone(keeper.getAssignedZone());
            if (z != null) {
                var bb = z.boundingBox();
                ref = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
            }
        }
        // 1. Une ZONE DE REPOS definie ? (le plus proche du centre de son enclos)
        restZone = findRestZone(ref);
        if (restZone != null) {
            var bb = restZone.boundingBox();
            locker = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
            return true;
        }
        // 2. Sinon : le Vestiaire le plus proche
        locker = KeeperLockerBlock.nearestLocker(keeper.level(), ref, SEARCH_RANGE);
        if (locker == null) {
            // Registre vide (ex: apres un redemarrage) : scan local peu frequent
            cooldown = 200;
            locker = scanNearby();
        }
        return locker != null;
    }

    /** La zone de repos (type 1) la plus proche d'une reference, ou null. */
    private com.lex3d.ultimatezootaming.zones.ZooZone findRestZone(BlockPos ref) {
        if (!(keeper.level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        com.lex3d.ultimatezootaming.zones.ZooZone best = null;
        double bd = SEARCH_RANGE * SEARCH_RANGE;
        for (var z : com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones()) {
            if (z.getZoneType() != 1) continue;
            var bb = z.boundingBox();
            BlockPos c = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
            double d = ref.distSqr(c);
            if (d < bd) { bd = d; best = z; }
        }
        return best;
    }

    /** Cherche un Vestiaire dans un rayon de 24 blocs autour du soigneur. */
    private BlockPos scanNearby() {
        BlockPos c = keeper.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-24, -4, -24), c.offset(24, 4, 24))) {
            if (keeper.level().getBlockState(p).getBlock() instanceof KeeperLockerBlock) {
                return p.immutable();
            }
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        if (locker == null) return false;
        // Toutes les 5s, verifie si du travail est apparu dans son enclos
        if (keeper.tickCount % 100 == 0 && keeper.getAssignedZone() != null && hasWorkInZone()) {
            return false; // il retourne bosser (Refill/Care prennent le relais)
        }
        return true;
    }

    /** L'enclos assigne a-t-il besoin du soigneur ? (malade ou mangeoire a remplir) */
    private boolean hasWorkInZone() {
        if (!(keeper.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        var zone = com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level)
                .getZone(keeper.getAssignedZone());
        if (zone == null) return false;
        // 1. Un animal malade ?
        boolean sick = !level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(), a -> a.isAlive() && zone.contains(a.blockPosition())
                        && a.getCapability(com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                            .resolve().map(d -> d.isTamed() && d.isSick()).orElse(false)).isEmpty();
        if (sick) return true;
        // 2. Une mangeoire a remplir ? (seulement si l'enclos a des animaux)
        boolean hasAnimals = !level.getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                zone.boundingBox(), a -> a.isAlive() && zone.contains(a.blockPosition())).isEmpty();
        if (!hasAnimals) return false;
        // Scan de la surface (sol +/- 3) uniquement, pas tout le volume (perf)
        for (long packed : zone.floorColumns()) {
            net.minecraft.core.BlockPos floor = net.minecraft.core.BlockPos.of(packed);
            for (int dy = -2; dy <= 3; dy++) {
                net.minecraft.core.BlockPos p = floor.above(dy);
                if (level.getBlockEntity(p) instanceof com.lex3d.ultimatezootaming.blocks.FeederBlockEntity fe
                        && fe.needsRefill()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (locker == null) return;
        double distSq = keeper.blockPosition().distSqr(locker);
        if (distSq > HOME_RADIUS * HOME_RADIUS) {
            // Trop loin du vestiaire : y retourner
            if (keeper.getNavigation().isDone()) {
                keeper.getNavigation().moveTo(locker.getX() + 0.5, locker.getY() + 1, locker.getZ() + 0.5, 1.0);
            }
        } else if (keeper.getNavigation().isDone() && keeper.getRandom().nextInt(60) == 0) {
            RandomSource r = keeper.getRandom();
            if (restZone != null) {
                // Zone de repos : il flane DANS la zone definie
                var bb = restZone.boundingBox();
                double x = bb.minX + r.nextDouble() * (bb.maxX - bb.minX);
                double z = bb.minZ + r.nextDouble() * (bb.maxZ - bb.minZ);
                keeper.getNavigation().moveTo(x, bb.minY, z, 1.0);
            } else {
                // Autour du vestiaire : rayon fixe
                double x = locker.getX() + 0.5 + (r.nextDouble() - 0.5) * HOME_RADIUS * 2;
                double z = locker.getZ() + 0.5 + (r.nextDouble() - 0.5) * HOME_RADIUS * 2;
                keeper.getNavigation().moveTo(x, locker.getY() + 1, z, 1.0);
            }
        }
    }

    @Override
    public void stop() {
        locker = null;
        restZone = null;
        keeper.getNavigation().stop();
    }
}
