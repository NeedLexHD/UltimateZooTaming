package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.BaitedTrapRegistry;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Sans ca, un mob sauvage n'a AUCUNE raison de se rapprocher d'une cage/filet
 * appate : il faudrait un coup de chance pur pour qu'il marche exactement dessus.
 * Ce Goal fait qu'un mob NON TAME cherche periodiquement (toutes les 2s, pas chaque
 * tick) s'il y a un piege appate dans un rayon de recherche, et si oui, marche vers
 * lui -- comme quand tu tiens du ble devant une vache.
 *
 * Ne s'applique jamais a un mob deja tame (on ne veut pas qu'un familier aille se
 * faire re-capturer par une cage qui traine).
 */
public class ZooAttractionGoal extends Goal {

    private static final double SEARCH_RADIUS = 10.0;
    private static final int RECHECK_INTERVAL = 40;   // 2 secondes
    private static final int MAX_ATTRACT_TICKS = 200; // 10s max : anti-blocage

    private final PathfinderMob mob;
    private BlockPos targetTrap;
    private int recheckTimer;
    private int attractTicks;

    public ZooAttractionGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (recheckTimer-- > 0) return false;
        recheckTimer = RECHECK_INTERVAL;

        boolean alreadyTamed = mob.getCapability(CapabilityHandler.TAMING_DATA)
                .map(TamingData::isTamed).orElse(false);
        if (alreadyTamed) return false;

        targetTrap = BaitedTrapRegistry.findNearestMatching(mob.level(), mob.blockPosition(), SEARCH_RADIUS, mob);
        return targetTrap != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetTrap == null) return false;

        // Le piege a disparu ou a ete vide (capture d'un autre mob, appat consomme...) :
        // on lache IMMEDIATEMENT la cible, sinon le mob reste verrouille sur une
        // position fantome et ne re-scanne plus jamais le registre.
        if (!BaitedTrapRegistry.isStillActive(mob.level(), targetTrap)) return false;

        // Timeout de securite : si le mob n'a pas atteint le piege en 10s (bouscule
        // par d'autres mobs, chemin difficile...), on abandonne ce cycle -- il pourra
        // re-cibler (le meme piege ou un plus proche) au prochain scan.
        if (attractTicks > MAX_ATTRACT_TICKS) return false;

        boolean alreadyTamed = mob.getCapability(CapabilityHandler.TAMING_DATA)
                .map(TamingData::isTamed).orElse(false);
        if (alreadyTamed) return false;

        Vec3 center = Vec3.atBottomCenterOf(targetTrap);
        return mob.distanceToSqr(center) > 1.0;
    }

    @Override
    public void start() {
        attractTicks = 0;
        moveToward();
    }

    @Override
    public void tick() {
        attractTicks++;
        moveToward();
    }

    private void moveToward() {
        if (targetTrap == null) return;
        if (mob.getNavigation().isDone()) {
            mob.getNavigation().moveTo(targetTrap.getX() + 0.5, targetTrap.getY(), targetTrap.getZ() + 0.5, 0.9);
        }
    }

    @Override
    public void stop() {
        targetTrap = null;
        mob.getNavigation().stop();
    }
}
