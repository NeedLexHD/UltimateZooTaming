package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;
import java.util.List;

/**
 * Gardien (metier 3) : quand un animal s'echappe, il accourt et reste a cote
 * pour le contenir (les visiteurs sont proteges) — mais il ne le recapture
 * PAS : ca, c'est le travail du directeur.
 */
public class GuardContainGoal extends Goal {

    private final ZooKeeperEntity keeper;
    private Animal target;
    private int cooldown;

    public GuardContainGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (keeper.getJob() != 3 || keeper.isOnStrike()) return false;
        if (--cooldown > 0) return false;
        cooldown = 40;
        List<Animal> escaped = keeper.level().getEntitiesOfClass(Animal.class,
                keeper.getBoundingBox().inflate(48),
                a -> a.isAlive() && a.getCapability(CapabilityHandler.TAMING_DATA)
                        .resolve().map(d -> d.isEscaped()).orElse(false));
        if (escaped.isEmpty()) return false;
        target = escaped.get(0);
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive()
                && target.getCapability(CapabilityHandler.TAMING_DATA)
                        .resolve().map(d -> d.isEscaped()).orElse(false);
    }

    @Override
    public void tick() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.GUARDING);
        if (target == null) return;
        double d = keeper.distanceToSqr(target);
        keeper.getLookControl().setLookAt(target.getX(), target.getEyeY(), target.getZ());
        if (d > 9.0) {
            if (keeper.getNavigation().isDone()) {
                keeper.getNavigation().moveTo(target, 1.15);
            }
        } else {
            keeper.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        target = null;
        keeper.getNavigation().stop();
    }
}
