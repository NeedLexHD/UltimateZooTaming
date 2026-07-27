package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Suivi de GROUPE : un accompagnant reste pres de son chef de groupe. Il ne
 * s'eloigne que si le chef est vraiment loin (alors il fait sa propre visite).
 * Le chef, lui, visite normalement (pas de leader) et les autres le suivent,
 * ce qui garde la famille ensemble a travers le parc.
 */
public class VisitorGroupGoal extends Goal {

    private final VisitorEntity visitor;
    private VisitorEntity leader;
    private int recheck;

    public VisitorGroupGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (visitor.getGroupLeader() == null || visitor.isLeaving()) return false;
        if (!(visitor.level() instanceof ServerLevel level)) return false;
        Entity e = level.getEntity(visitor.getGroupLeader());
        if (!(e instanceof VisitorEntity l) || !l.isAlive() || l.isLeaving()) return false;
        this.leader = l;
        // Ne suit que si le chef est a distance moyenne (sinon il visite tout seul :
        // le cerveau reprend la main). Ne suit que si VRAIMENT distance (>4.5 blocs)
        // pour eviter de se recoller sans arret (effet file indienne).
        double d = visitor.distanceToSqr(l);
        return d > 20.0 && d < 400.0;
    }

    @Override
    public boolean canContinueToUse() {
        if (leader == null || !leader.isAlive() || leader.isLeaving() || visitor.isLeaving()) return false;
        double d = visitor.distanceToSqr(leader);
        return d > 9.0 && d < 400.0; // laquer le suivi une fois a ~3 blocs du chef
    }

    @Override
    public void tick() {
        if (leader == null) return;
        visitor.getLookControl().setLookAt(leader, 20f, 20f);
        if (--recheck <= 0) {
            recheck = 25;
            double d2 = visitor.distanceToSqr(leader);
            // Assez proche du chef : on ne se colle pas, on flane sur place (le
            // cerveau/regard s'occupe du reste). Evite l'effet "train".
            if (d2 < 9.0) {  // < 3 blocs
                visitor.getNavigation().stop();
                return;
            }
            // Sinon : rejoindre un point A COTE du chef (pas dessus), propre a
            // chaque membre, pour former une petite GRAPPE et non une file.
            java.util.Random rng = new java.util.Random(visitor.getUUID().getLeastSignificantBits());
            double angle = rng.nextDouble() * Math.PI * 2;      // direction fixe par membre
            double radius = 1.6 + rng.nextDouble() * 1.2;        // 1.6 a 2.8 blocs du chef
            double tx = leader.getX() + Math.cos(angle) * radius;
            double tz = leader.getZ() + Math.sin(angle) * radius;
            double speed = visitor.isChild() ? 1.15 : 1.0;      // enfants trottinent
            visitor.getNavigation().moveTo(tx, leader.getY(), tz, speed);
        }
    }

    @Override
    public void stop() {
        leader = null;
        visitor.getNavigation().stop();
    }
}
