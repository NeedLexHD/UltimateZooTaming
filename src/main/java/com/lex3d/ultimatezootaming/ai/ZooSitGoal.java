package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Priorite 0 (la plus dominante) : des que "Assis" est actif, on coupe le
 * mouvement du mob QUOI QU'IL ARRIVE, meme si d'autres IA (vanille ou d'un
 * autre mod) veulent le faire bouger. C'est volontairement agressif -- sans
 * ca, un mob avec sa propre IA de deplacement (Polly's Pets, Alex's Mobs...)
 * continuerait a errer meme "assis".
 */
public class ZooSitGoal extends Goal {

    private final PathfinderMob mob;

    public ZooSitGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return mob.getCapability(CapabilityHandler.TAMING_DATA)
                .map(d -> d.isTamed() && d.isSitting())
                .orElse(false);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        mob.getNavigation().stop();
    }
}
