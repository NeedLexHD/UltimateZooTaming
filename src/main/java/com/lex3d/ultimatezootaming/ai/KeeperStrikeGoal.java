package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * URGENCE GREVE : un employe impaye cesse tout travail et vient se planter
 * devant la Tresorerie avec les autres grevistes, en affichant sa colere.
 *
 * Avant, une greve etait invisible : l'employe restait simplement inactif dans
 * son coin. Maintenant le probleme se voit d'un coup d'oeil depuis la caisse.
 *
 * Cout TPS negligeable : une seule destination pour tout le monde, recherchee
 * une fois au demarrage puis mise en cache.
 */
public class KeeperStrikeGoal extends Goal {

    /** Distance a laquelle on considere l'employe arrive au piquet de greve. */
    private static final double ARRIVED = 9.0; // 3 blocs au carre

    private final ZooKeeperEntity keeper;
    private BlockPos vaultPos;
    private int emoteTicks;

    public KeeperStrikeGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!keeper.isOnStrike()) return false;
        if (!(keeper.level() instanceof ServerLevel level)) return false;
        var vault = ZooVaultBlock.nearestVault(level, keeper.blockPosition(), 128);
        if (vault == null) return false; // aucune tresorerie : il rale sur place
        vaultPos = vault.getBlockPos();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return keeper.isOnStrike() && vaultPos != null;
    }

    @Override
    public void start() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.STRIKE);
        keeper.setWorking(false);
        emoteTicks = 0;
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        vaultPos = null;
        emoteTicks = 0;
        keeper.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (vaultPos == null) return;
        if (!(keeper.level() instanceof ServerLevel level)) return;

        double dist = keeper.blockPosition().distSqr(vaultPos);
        if (dist > ARRIVED) {
            // On rejoint le piquet de greve, sans se presser
            keeper.getNavigation().moveTo(vaultPos.getX() + 0.5, vaultPos.getY(),
                    vaultPos.getZ() + 0.5, 0.85);
        } else {
            // Sur place : il fixe la caisse d'un air mauvais
            keeper.getNavigation().stop();
            keeper.getLookControl().setLookAt(
                    vaultPos.getX() + 0.5, vaultPos.getY() + 1.0, vaultPos.getZ() + 0.5);
        }

        // Emote de colere reguliere, ou qu'il soit sur le trajet
        emoteTicks++;
        if (emoteTicks % 30 == 0) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                    keeper.getX(), keeper.getEyeY() + 0.5, keeper.getZ(),
                    1, 0.1, 0.1, 0.1, 0.0);
        }
        // Rale de temps en temps une fois arrive
        if (dist <= ARRIVED && emoteTicks % 120 == 0) {
            level.playSound(null, keeper.blockPosition(),
                    net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 0.9f);
        }
    }
}
