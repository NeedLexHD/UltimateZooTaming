package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * RYTHME DE LA JOURNEE DE TRAVAIL :
 *
 *  - PRISE DE POSTE : a l'ouverture du zoo, l'employe quitte la salle de repos
 *    et rejoint son premier enclos assigne (ou l'entree s'il n'en a pas). Il
 *    fait vraiment le trajet au lieu d'apparaitre a son poste.
 *
 *  - PAUSE DEJEUNER : vers le milieu de journee, il retourne quelques instants
 *    a la salle de repos. Le parc tourne au ralenti pendant ce temps.
 *
 *  - FIN DE SERVICE : au crepuscule, il rentre a la salle de repos avant que
 *    le Goal de sommeil ne prenne le relais pour le coucher.
 */
public class KeeperRoutineGoal extends Goal {

    /** Debut et fin de la fenetre de pause dejeuner (temps du monde). */
    private static final long LUNCH_START = 5800L, LUNCH_END = 6600L;
    /** Fenetre de prise de poste, juste apres l'aube. */
    private static final long OPENING_END = 1200L;
    /** Fenetre de fin de service, avant la nuit. */
    private static final long CLOSING_START = 11200L;

    /** Ce que l'employe est en train de faire. */
    private enum Phase { NONE, TO_POST, TO_LUNCH, TO_HOME }

    private final ZooKeeperEntity keeper;
    private Phase phase = Phase.NONE;
    private BlockPos target;
    private int ticks;
    /** Jour du dernier trajet de chaque type, pour n'en faire qu'un par jour. */
    private long lastPostDay = -1, lastLunchDay = -1, lastHomeDay = -1;

    public KeeperRoutineGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (keeper.isOnStrike() || keeper.isSleeping()) return false;
        if (!(keeper.level() instanceof ServerLevel level)) return false;

        long time = level.getDayTime() % 24000L;
        long day = level.getDayTime() / 24000L;

        // 1. PRISE DE POSTE au petit matin
        if (time < OPENING_END && lastPostDay != day) {
            target = findPost(level);
            if (target != null) {
                phase = Phase.TO_POST;
                lastPostDay = day;
                ticks = 0;
                return true;
            }
        }
        // 2. PAUSE DEJEUNER en milieu de journee
        if (time >= LUNCH_START && time < LUNCH_END && lastLunchDay != day) {
            target = findRestZone(level);
            if (target != null) {
                phase = Phase.TO_LUNCH;
                lastLunchDay = day;
                ticks = 0;
                return true;
            }
        }
        // 3. FIN DE SERVICE au crepuscule
        if (time >= CLOSING_START && lastHomeDay != day) {
            target = findRestZone(level);
            if (target != null) {
                phase = Phase.TO_HOME;
                lastHomeDay = day;
                ticks = 0;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (phase == Phase.NONE || target == null) return false;
        if (keeper.isOnStrike()) return false;
        // La pause dure un moment une fois sur place ; les trajets ont un timeout
        int limit = (phase == Phase.TO_LUNCH) ? 900 : 500;
        return ticks < limit;
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        phase = Phase.NONE;
        target = null;
        ticks = 0;
        keeper.setWorking(false);
        keeper.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (target == null) return;
        ticks++;
        // La routine couvre trois moments differents : on l'affiche precisement
        keeper.setTask(phase == Phase.TO_LUNCH ? com.lex3d.ultimatezootaming.entities.KeeperTask.BREAK : com.lex3d.ultimatezootaming.entities.KeeperTask.COMMUTE);
        double dist = keeper.blockPosition().distSqr(target);

        if (dist > 6.0) {
            keeper.setWorking(false);
            keeper.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.9);
            // Chemin impossible : on abandonne la routine plutot que de bloquer le travail
            if (keeper.getNavigation().isDone() && ticks > 200) stop();
            return;
        }

        // Arrive : pendant la pause il flane sur place, sinon la routine est finie
        keeper.getNavigation().stop();
        if (phase == Phase.TO_LUNCH) {
            keeper.setWorking(false);
            // Petit mouvement de tete pour ne pas rester fige comme une statue
            if (ticks % 40 == 0) {
                keeper.getLookControl().setLookAt(
                        keeper.getX() + keeper.getRandom().nextDouble() * 4 - 2,
                        keeper.getEyeY(),
                        keeper.getZ() + keeper.getRandom().nextDouble() * 4 - 2);
            }
        } else {
            stop();
        }
    }

    /** Son poste : le centre de son premier enclos, sinon l'entree du zoo. */
    private BlockPos findPost(ServerLevel level) {
        for (var id : keeper.getAssignedZones()) {
            ZooZone z = ZooSavedData.get(level).getZone(id);
            if (z == null) continue;
            var bb = z.boundingBox();
            return BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
        }
        return null; // sans enclos assigne, pas de prise de poste dediee
    }

    /** La salle de repos la plus proche (elle sert de refectoire et de dortoir). */
    private BlockPos findRestZone(ServerLevel level) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (ZooZone z : ZooSavedData.get(level).getAllZones()) {
            if (z.getZoneType() != 1) continue;
            var bb = z.boundingBox();
            BlockPos c = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
            double d = keeper.blockPosition().distSqr(c);
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }
}
