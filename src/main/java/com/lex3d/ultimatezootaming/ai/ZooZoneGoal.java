package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Random;
import java.util.UUID;

/**
 * Le mob assigne a un enclos erre librement DANS SA FORME EXACTE (les colonnes
 * de sol scannees), et est ramene s'il en sort. Teleport de secours s'il reste
 * coince dehors trop longtemps (ex: pousse a travers la cloture par un autre mob).
 */
public class ZooZoneGoal extends Goal {

    private static final int RECALC_INTERVAL = 40;       // logique toutes les 2s
    private static final int TELEPORT_AFTER_TICKS = 200; // 10s hors zone -> teleport

    private static final Random RANDOM = new Random();

    private final PathfinderMob mob;
    private int recalcTimer;
    private int outsideTicks;

    public ZooZoneGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private ZooZone getZone() {
        var data = mob.getCapability(CapabilityHandler.TAMING_DATA).resolve().orElse(null);
        if (data == null || !data.isTamed()) return null;
        UUID zoneId = data.getZoneId();
        if (zoneId == null || !(mob.level() instanceof ServerLevel serverLevel)) return null;
        return ZooSavedData.get(serverLevel).getZone(zoneId);
    }

    @Override
    public boolean canUse() {
        return getZone() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        recalcTimer = 0;
        outsideTicks = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        outsideTicks = 0;
    }

    @Override
    public void tick() {
        ZooZone zone = getZone();
        if (zone == null) return;

        // Delai de grace : juste apres un rechargement de chunk, l'animal reapparait
        // a sa vraie position sauvegardee. On NE le repositionne PAS pendant les
        // premieres secondes (evite l'effet "tous regroupes puis re-repartis").
        if (mob.tickCount < 100) {
            outsideTicks = 0;
            return;
        }

        boolean inside = zone.contains(mob.blockPosition());
        if (!inside) {
            outsideTicks++;
        } else {
            outsideTicks = 0;
        }

        recalcTimer--;
        if (recalcTimer > 0) return;
        recalcTimer = RECALC_INTERVAL;

        if (!inside) {
            if (outsideTicks > TELEPORT_AFTER_TICKS) {
                // Retour par TP sur une case ALEATOIRE (evite l'empilement au meme point
                // quand plusieurs animaux reviennent en meme temps apres un rechargement)
                BlockPos spot = zone.randomFloorPos(RANDOM);
                mob.moveTo(spot.getX() + 0.5, spot.getY() + 1, spot.getZ() + 0.5,
                        mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                outsideTicks = 0;
            } else {
                BlockPos back = zone.nearestFloorPos(mob.blockPosition());
                if (back != null) {
                    mob.getNavigation().moveTo(back.getX() + 0.5, back.getY() + 1, back.getZ() + 0.5, 1.1);
                }
            }
        } else if (RANDOM.nextFloat() < 0.35f && mob.getNavigation().isDone()) {
            // Errance naturelle : une case aleatoire de l'enclos (donc toujours dedans)
            BlockPos wander = zone.randomFloorPos(RANDOM);
            mob.getNavigation().moveTo(wander.getX() + 0.5, wander.getY() + 1, wander.getZ() + 0.5, 0.8);
        }
    }
}
