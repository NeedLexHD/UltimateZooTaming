package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.Random;

/**
 * Balade du Soigneur DANS son enclos assigne (au lieu d'errer n'importe ou avec
 * le stroll vanilla). Il choisit des cases de sol aleatoires de l'enclos, et est
 * ramene s'il en sort. Sans enclos assigne, ce goal ne fait rien (le stroll
 * vanilla de fallback prend alors le relais).
 */
public class KeeperStrollGoal extends Goal {

    private static final int RECALC = 50;
    private static final int TELEPORT_AFTER = 200;
    private static final Random RNG = new Random();

    private final ZooKeeperEntity keeper;
    private int timer;
    private int outsideTicks;

    public KeeperStrollGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private ZooZone getZone() {
        if (keeper.getAssignedZone() == null || !(keeper.level() instanceof ServerLevel level)) return null;
        return ZooSavedData.get(level).getZone(keeper.getAssignedZone());
    }

    @Override
    public boolean canUse() {
        return getZone() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return getZone() != null;
    }

    @Override
    public void tick() {
        ZooZone zone = getZone();
        if (zone == null) return;

        boolean inside = zone.contains(keeper.blockPosition());
        outsideTicks = inside ? 0 : outsideTicks + 1;

        if (--timer > 0) return;
        timer = RECALC;

        if (!inside) {
            BlockPos back = zone.nearestFloorPos(keeper.blockPosition());
            if (back != null) {
                if (outsideTicks > TELEPORT_AFTER) {
                    keeper.moveTo(back.getX() + 0.5, back.getY() + 1, back.getZ() + 0.5,
                            keeper.getYRot(), keeper.getXRot());
                    keeper.getNavigation().stop();
                    outsideTicks = 0;
                } else {
                    keeper.getNavigation().moveTo(back.getX() + 0.5, back.getY() + 1, back.getZ() + 0.5, 1.0);
                }
            }
        } else if (RNG.nextFloat() < 0.5f && keeper.getNavigation().isDone()) {
            BlockPos wander = zone.randomFloorPos(RNG);
            keeper.getNavigation().moveTo(wander.getX() + 0.5, wander.getY() + 1, wander.getZ() + 0.5, 1.0);
        }
    }
}
