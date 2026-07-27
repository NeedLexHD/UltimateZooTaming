package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * L'employe REAGIT au directeur (joueur proche) : il se tourne vers lui et fait
 * un petit signe (particules) quand le joueur passe pres de lui, une fois de
 * temps en temps (pas en boucle). Donne vie au personnel.
 */
public class KeeperGreetGoal extends Goal {

    private final ZooKeeperEntity keeper;
    private Player nearby;
    private int greetCd;      // pour ne pas saluer en boucle
    private int lookTicks;

    public KeeperGreetGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (greetCd > 0) { greetCd--; return false; }
        nearby = keeper.level().getNearestPlayer(keeper, 4.0);
        return nearby != null && !nearby.isSpectator();
    }

    @Override
    public boolean canContinueToUse() {
        return nearby != null && nearby.isAlive() && lookTicks < 30
                && keeper.distanceToSqr(nearby) < 36.0;
    }

    @Override
    public void start() {
        lookTicks = 0;
        // Petit signe : particules "content" au-dessus de la tete
        if (keeper.level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    keeper.getX(), keeper.getEyeY() + 0.6, keeper.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @Override
    public void tick() {
        lookTicks++;
        if (nearby != null) {
            keeper.getLookControl().setLookAt(nearby, 30f, 30f);
        }
    }

    @Override
    public void stop() {
        nearby = null;
        greetCd = 200 + keeper.getRandom().nextInt(200); // ~10-20s avant de resaluer
    }
}
