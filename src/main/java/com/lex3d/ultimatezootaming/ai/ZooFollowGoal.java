package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.capability.TamingData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 * Suivi continu, calque sur le FollowOwnerGoal vanilla (loups/chats) :
 * - Demarre a courte distance (3 blocs), s'arrete quand assez proche (2 blocs) --
 *   hysteresis pour eviter que le mob demarre/stoppe en boucle.
 * - Recalcule le chemin toutes les 10 ticks (pas chaque tick, cf. perf).
 * - Si le proprietaire est VRAIMENT loin (au-dela du WanderRadius regle dans le
 *   Sifflet), on teleporte le mob pres de lui plutot que de le laisser marcher
 *   indefiniment (utile apres un Nether/End, une chute, etc.)
 *
 * WanderRadius devient donc la distance MAX avant teleportation de rattrapage,
 * pas un seuil de demarrage du suivi (qui lui est fixe a 3 blocs, comme un vrai
 * animal de compagnie).
 */
public class ZooFollowGoal extends Goal {

    private static final double START_FOLLOW_DIST_SQ = 3.0 * 3.0;
    private static final double STOP_FOLLOW_DIST_SQ = 2.0 * 2.0;
    private static final int RECALC_INTERVAL = 10;

    private final PathfinderMob mob;
    private Player owner;
    private int recalcTimer;

    public ZooFollowGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return mob.getCapability(CapabilityHandler.TAMING_DATA).map(this::evaluate).orElse(false);
    }

    private boolean evaluate(TamingData data) {
        if (!data.isTamed() || data.isSitting()) return false;
        // Un animal ASSIGNE a un enclos ne suit JAMAIS le proprietaire : il reste
        // dans sa zone (c'est ZooZoneGoal qui le gere). Evite le TP vers le joueur
        // au rechargement de chunk.
        if (data.getZoneId() != null) return false;
        Player candidate = mob.level().getPlayerByUUID(data.getOwnerUUID());
        if (candidate == null) return false;
        this.owner = candidate;
        return mob.distanceToSqr(owner) > START_FOLLOW_DIST_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        if (owner == null || !owner.isAlive()) return false;
        // Coupe immediatement si l'animal vient d'etre assigne a un enclos
        boolean assigned = mob.getCapability(CapabilityHandler.TAMING_DATA)
                .map(d -> d.getZoneId() != null).orElse(false);
        if (assigned) return false;
        boolean stillSitting = mob.getCapability(CapabilityHandler.TAMING_DATA)
                .map(TamingData::isSitting).orElse(true);
        if (stillSitting) return false;
        return mob.distanceToSqr(owner) > STOP_FOLLOW_DIST_SQ;
    }

    @Override
    public void start() {
        recalcTimer = 0;
    }

    @Override
    public void stop() {
        owner = null;
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (owner == null) return;
        mob.getLookControl().setLookAt(owner, 10.0f, mob.getMaxHeadXRot());

        recalcTimer--;
        if (recalcTimer > 0) return;
        recalcTimer = RECALC_INTERVAL;

        double wanderRadius = mob.getCapability(CapabilityHandler.TAMING_DATA)
                .map(TamingData::getWanderRadius).orElse(16.0);

        if (mob.distanceToSqr(owner) > wanderRadius * wanderRadius) {
            tryToTeleportToOwner();
        } else {
            mob.getNavigation().moveTo(owner, 1.15);
        }
    }

    private void tryToTeleportToOwner() {
        BlockPos ownerPos = owner.blockPosition();
        for (int i = 0; i < 10; i++) {
            int dx = mob.getRandom().nextInt(7) - 3;
            int dz = mob.getRandom().nextInt(7) - 3;
            BlockPos candidate = ownerPos.offset(dx, 0, dz);
            if (Math.abs(candidate.getX() - owner.getX()) < 2.0 && Math.abs(candidate.getZ() - owner.getZ()) < 2.0) {
                continue; // trop pres du joueur, ca collisionnerait
            }
            if (canTeleportTo(candidate)) {
                mob.moveTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5,
                        mob.getYRot(), mob.getXRot());
                mob.getNavigation().stop();
                return;
            }
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        Level level = mob.level();
        BlockState below = level.getBlockState(pos.below());
        if (!below.isFaceSturdy(level, pos.below(), Direction.UP)) return false;

        BlockPos offset = pos.subtract(mob.blockPosition());
        return level.noCollision(mob, mob.getBoundingBox().move(offset.getX(), offset.getY(), offset.getZ()));
    }
}
