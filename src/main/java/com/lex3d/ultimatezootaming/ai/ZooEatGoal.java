package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.FeederBlockEntity;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.UUID;

/**
 * L'animal apprivoise se deplace vers une mangeoire de SON regime, ou qu'elle
 * soit dans l'enclos (meme a l'autre bout), pour aller manger. Sans ce goal,
 * les animaux ne bougeaient jamais vers la nourriture.
 */
public class ZooEatGoal extends Goal {

    private final PathfinderMob mob;
    private BlockPos feederPos;
    private int cooldown;
    private int eatTicks;

    public ZooEatGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private ZooZone getZone() {
        var data = mob.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (data == null) return null;
        UUID zoneId = data.getZoneId();
        if (zoneId == null || !(mob.level() instanceof ServerLevel sl)) return null;
        return ZooSavedData.get(sl).getZone(zoneId);
    }

    @Override
    public boolean canUse() {
        if (--cooldown > 0) return false;
        cooldown = 60 + mob.getRandom().nextInt(80);
        if (!(mob.level() instanceof ServerLevel level)) return false;
        ZooZone zone = getZone();
        if (zone == null) return false;

        // Cherche la mangeoire du bon regime la plus proche DANS l'enclos.
        // Scan de la SURFACE uniquement (colonnes du sol +/- 3), pas tout le
        // volume de 40 blocs de haut (sinon freeze serveur sur grand enclos).
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (long packed : zone.floorColumns()) {
            BlockPos floor = BlockPos.of(packed);
            for (int dy = -2; dy <= 3; dy++) {
                BlockPos p = floor.above(dy);
                if (level.getBlockEntity(p) instanceof FeederBlockEntity feeder && feeder.hasFood()
                        && feeder.storedItem() instanceof com.lex3d.ultimatezootaming.items.FodderItem fodder
                        && fodder.getDiet().matches(mob)) {
                    double d = p.distSqr(mob.blockPosition());
                    if (d < bestD) { bestD = d; best = p.immutable(); }
                }
            }
        }
        if (best == null) return false;
        feederPos = best;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return feederPos != null && eatTicks < 100
                && mob.level().getBlockEntity(feederPos) instanceof FeederBlockEntity f && f.hasFood();
    }

    @Override
    public void start() {
        eatTicks = 0;
        if (feederPos != null) {
            mob.getNavigation().moveTo(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5, 1.0);
        }
    }

    @Override
    public void tick() {
        if (feederPos == null) return;
        mob.getLookControl().setLookAt(feederPos.getX() + 0.5, feederPos.getY() + 0.5, feederPos.getZ() + 0.5);
        double dist = mob.distanceToSqr(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5);
        if (dist > 2.5) {
            if (mob.getNavigation().isDone()) {
                mob.getNavigation().moveTo(feederPos.getX() + 0.5, feederPos.getY(), feederPos.getZ() + 0.5, 1.0);
            }
        } else {
            // A la mangeoire : mange (gain de satisfaction)
            mob.getNavigation().stop();
            eatTicks++;
            if (eatTicks % 20 == 0) {
                mob.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(d ->
                        d.setSatisfaction(Math.min(100, d.getSatisfaction() + 1)));
                if (mob.level() instanceof ServerLevel sl) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            mob.getX(), mob.getEyeY(), mob.getZ(), 1, 0.2, 0.2, 0.2, 0);
                }
            }
        }
    }

    @Override
    public void stop() {
        feederPos = null;
        eatTicks = 0;
        mob.getNavigation().stop();
    }
}
