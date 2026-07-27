package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.EnumSet;
import java.util.List;

/**
 * RONDE credible d'un employe quand il n'a pas de tache urgente. Le comportement
 * s'adapte au metier :
 *  - Veterinaire : fait le tour de SON enclos, s'arrete pres des animaux comme
 *    s'il les examinait (regarde chacun un instant).
 *  - Nourrisseur : circule entre les enclos (tour du parc).
 *  - Gardien : patrouille largement (deja gere par sa contention, ici il erre).
 *  - Polyvalent : ronde dans son enclos assigne.
 * Remplace l'ancien "stroll" au hasard par des deplacements qui ont du sens.
 */
public class KeeperPatrolGoal extends Goal {

    private final ZooKeeperEntity keeper;
    private int repathCd;
    private int pauseTicks;          // pause d'observation devant un animal
    private Animal observed;
    private BlockPos waypoint;

    public KeeperPatrolGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (keeper.isOnStrike()) return false;
        // Ne patrouille que le jour (la nuit = sommeil), et jamais pour le vendeur
        // (il tient sa caisse) ni pendant une greve.
        if (!(keeper.level() instanceof ServerLevel level) || !level.isDay()) return false;
        return keeper.getJob() != 4;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.PATROL);
        if (!(keeper.level() instanceof ServerLevel level)) return;

        // En pause d'observation devant un animal : le regarder
        if (pauseTicks > 0) {
            pauseTicks--;
            if (observed != null && observed.isAlive()) {
                keeper.getLookControl().setLookAt(observed, 30f, 30f);
            }
            keeper.getNavigation().stop();
            return;
        }

        if (--repathCd > 0) return;
        repathCd = 40;

        ZooZone zone = getPatrolZone(level);
        if (zone == null) return;

        // Le veterinaire/polyvalent inspecte les animaux de son enclos
        if ((keeper.getJob() == 1 || keeper.getJob() == 0) && keeper.getRandom().nextBoolean()) {
            List<Animal> animals = level.getEntitiesOfClass(Animal.class,
                    zone.boundingBox(), a -> a.isAlive() && zone.contains(a.blockPosition()));
            if (!animals.isEmpty()) {
                Animal a = animals.get(keeper.getRandom().nextInt(animals.size()));
                double d = keeper.distanceToSqr(a);
                if (d > 4.0) {
                    keeper.getNavigation().moveTo(a, 1.0);
                } else {
                    // Arrive pres de l'animal : pause d'inspection ~2-4s
                    observed = a;
                    pauseTicks = 40 + keeper.getRandom().nextInt(40);
                }
                return;
            }
        }

        // Sinon : deplacement vers un point de la zone (ronde)
        if (keeper.getNavigation().isDone()) {
            BlockPos wp = zone.randomFloorPos(new java.util.Random());
            if (wp != null) {
                keeper.getNavigation().moveTo(wp.getX() + 0.5, wp.getY() + 1, wp.getZ() + 0.5, 0.8);
            }
        }
    }

    /** Index de rotation entre les enclos assignes (tournee d'inspection). */
    private int patrolIndex = 0;

    /** L'enclos a patrouiller selon le metier. */
    private ZooZone getPatrolZone(ServerLevel level) {
        // Nourrisseur / Gardien : un enclos au hasard du parc (ils circulent partout)
        if (keeper.getJob() == 2 || keeper.getJob() == 3) {
            List<ZooZone> zones = new java.util.ArrayList<>();
            for (ZooZone z : ZooSavedData.get(level).getAllZones()) if (z.isAnimalZone()) zones.add(z);
            if (zones.isEmpty()) return null;
            return zones.get(keeper.getRandom().nextInt(zones.size()));
        }
        // Veterinaire / Polyvalent : ils tournent entre TOUS leurs enclos (max 3).
        // A chaque cycle de ronde, on passe au suivant : c'est ce qui cree la
        // vraie tournee d'inspection quand plusieurs enclos sont assignes.
        var owned = keeper.getAssignedZones();
        if (owned.isEmpty()) return null;
        if (owned.size() == 1) return ZooSavedData.get(level).getZone(owned.get(0));
        patrolIndex = (patrolIndex + 1) % owned.size();
        ZooZone z = ZooSavedData.get(level).getZone(owned.get(patrolIndex));
        // L'enclos a pu etre supprime entre-temps : on prend le premier valide
        if (z == null) {
            for (var id : owned) {
                z = ZooSavedData.get(level).getZone(id);
                if (z != null) break;
            }
        }
        return z;
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        observed = null;
        pauseTicks = 0;
        keeper.getNavigation().stop();
    }
}
