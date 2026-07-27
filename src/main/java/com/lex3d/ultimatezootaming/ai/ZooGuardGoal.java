package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * "Garder ici" : le mob est ancre a un point fixe (fige au moment de la commande,
 * voir WhistleCommandC2SPacket#GUARD) et erre librement autour de ce point, dans
 * un rayon precis (le WanderRadius du Sifflet), SANS jamais s'en eloigner plus que
 * ca ni despawn (persistenceRequired est deja force a true des le taming, voir
 * TamingUtil).
 */
public class ZooGuardGoal extends Goal {

    private static final int RECALC_INTERVAL = 30; // toutes les 1.5s, pas besoin de plus frequent pour un garde passif

    private final PathfinderMob mob;
    private int recalcTimer;

    public ZooGuardGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.getCapability(CapabilityHandler.TAMING_DATA)
                .map(d -> d.isTamed() && d.isGuarding())
                .orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        recalcTimer = 0;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        recalcTimer--;
        if (recalcTimer > 0) return;
        recalcTimer = RECALC_INTERVAL;

        mob.getCapability(CapabilityHandler.TAMING_DATA).ifPresent(data -> {
            BlockPos guardPos = data.getGuardPos();
            if (guardPos == null) return;

            double distSq = mob.distanceToSqr(guardPos.getX() + 0.5, guardPos.getY(), guardPos.getZ() + 0.5);
            double radius = data.getWanderRadius();

            if (distSq > radius * radius) {
                // Trop loin du point de garde : on rentre directement au point, pas de wander aleatoire
                mob.getNavigation().moveTo(guardPos.getX() + 0.5, guardPos.getY(), guardPos.getZ() + 0.5, 1.0);
            } else if (mob.getRandom().nextFloat() < 0.3f) {
                // Sinon, petite chance de deambuler aleatoirement DANS le rayon (comportement naturel)
                double angle = mob.getRandom().nextDouble() * Math.PI * 2;
                double dist = mob.getRandom().nextDouble() * radius;
                double tx = guardPos.getX() + 0.5 + Math.cos(angle) * dist;
                double tz = guardPos.getZ() + 0.5 + Math.sin(angle) * dist;
                mob.getNavigation().moveTo(tx, guardPos.getY(), tz, 0.8);
            }
        });
    }
}
