package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooDormitory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.EnumSet;

/**
 * La nuit (zoo ferme), l'employe rejoint le DORTOIR et dort dans un lit vanilla
 * libre. Le jour, il retourne bosser (ce goal se desactive). Routine de parc.
 */
public class KeeperSleepGoal extends Goal {

    private final ZooKeeperEntity keeper;
    private BlockPos bed;
    private int repathCd;

    public KeeperSleepGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (keeper.isOnStrike()) return false;
        if (!(keeper.level() instanceof ServerLevel level)) return false;
        if (level.isDay()) return false; // on ne dort que la nuit
        // DORTOIR = le dortoir du Selecteur OU n'importe quelle zone "Salle de repos".
        // Avant, seul le Selecteur comptait : si tu avais juste une zone Salle de
        // repos avec des lits, personne n'allait dormir.
        bed = findFreeBed(level, ZooDormitory.get(level));
        if (bed != null) return true;
        restCenter = findRestZoneCenter(level);
        ZooDormitory dorm = ZooDormitory.get(level);
        return restCenter != null || dorm.center() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return keeper.level() instanceof ServerLevel level && !level.isDay() && !keeper.isOnStrike();
    }

    @Override
    public void tick() {
        if (!(keeper.level() instanceof ServerLevel level)) return;
        // Si pas encore de lit, on en cherche un a chaque passage (un lit peut se liberer)
        if (bed == null) {
            bed = findFreeBed(level, ZooDormitory.get(level));
        }
        BlockPos target = bed;
        if (target == null) target = restCenter;
        if (target == null) target = ZooDormitory.get(level).center();
        if (target == null) return;
        double dist = keeper.distanceToSqr(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        if (dist > 2.5) {
            if (--repathCd <= 0 && keeper.getNavigation().isDone()) {
                keeper.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
                repathCd = 40;
            }
        } else {
            keeper.getNavigation().stop();
            // Posture couchee UNIQUEMENT si on est sur un vrai lit (sinon reste debout)
            if (bed != null) {
                keeper.setPose(net.minecraft.world.entity.Pose.SLEEPING);
            }
        }
    }

    @Override
    public void stop() {
        keeper.setPose(net.minecraft.world.entity.Pose.STANDING);
        bed = null;
    }

    /** Un lit (partie tete) libre dans le dortoir. */
    /** Centre de la zone "Salle de repos" la plus proche (fallback sans lit). */
    private BlockPos restCenter;

    /**
     * Cherche un lit libre : d'abord dans le dortoir du Selecteur, puis dans
     * TOUTES les zones de type "Salle de repos" (type 1). Un lit est "libre"
     * si aucun autre employe ne dort deja dessus.
     */
    private BlockPos findFreeBed(ServerLevel level, ZooDormitory dorm) {
        // 1. Le dortoir defini au Selecteur de parcelle
        var box = dorm.bounds();
        if (box != null) {
            BlockPos found = scanForBed(level,
                    (int) box.minX, (int) box.minY, (int) box.minZ,
                    (int) box.maxX, (int) box.maxY, (int) box.maxZ);
            if (found != null) return found;
        }
        // 2. Les zones "Salle de repos" (elles servent de dortoir + stockage)
        for (var z : com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones()) {
            if (z.getZoneType() != 1) continue;
            var bb = z.boundingBox();
            BlockPos found = scanForBed(level,
                    (int) bb.minX, (int) bb.minY, (int) bb.minZ,
                    (int) bb.maxX, (int) bb.maxY, (int) bb.maxZ);
            if (found != null) return found;
        }
        return null;
    }

    /** Scanne une boite pour un lit dont la tete est libre (pas d'employe dessus). */
    private BlockPos scanForBed(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2) {
        // Garde-fou perf : on limite la hauteur scannee
        int minY = Math.max(y1, y1);
        int maxY = Math.min(y2, y1 + 8);
        for (BlockPos p : BlockPos.betweenClosed(x1, minY, z1, x2, maxY, z2)) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof BedBlock)) continue;
            if (st.getValue(BedBlock.PART) != BedPart.HEAD) continue;
            // Lit deja occupe par un autre employe ?
            BlockPos bp = p.immutable();
            boolean taken = !level.getEntitiesOfClass(
                    com.lex3d.ultimatezootaming.entities.ZooKeeperEntity.class,
                    new net.minecraft.world.phys.AABB(bp).inflate(1.2),
                    k -> k != keeper && k.isSleeping()).isEmpty();
            if (!taken) return bp;
        }
        return null;
    }

    /** Centre de la zone "Salle de repos" la plus proche du soigneur. */
    private BlockPos findRestZoneCenter(ServerLevel level) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (var z : com.lex3d.ultimatezootaming.saveddata.ZooSavedData.get(level).getAllZones()) {
            if (z.getZoneType() != 1) continue;
            var bb = z.boundingBox();
            BlockPos c = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
            double d = keeper.blockPosition().distSqr(c);
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }
}
