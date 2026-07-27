package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.LitterBlock;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * AGENT D'ENTRETIEN : parcourt le parc et ramasse les detritus laisses par les
 * visiteurs. Chaque dechet ramasse produit un Dechet recyclable, depose dans le
 * vestiaire le plus proche (ou au sol si aucun vestiaire).
 *
 * Le polyvalent nettoie aussi, mais seulement dans ses enclos assignes.
 */
public class KeeperCleanGoal extends Goal {

    /** Rayon de recherche des detritus autour de l'employe. */
    private static final int BASE_SEARCH = 24;

    private final ZooKeeperEntity keeper;
    private BlockPos litter;
    private int cooldown;
    private int travelTicks;

    public KeeperCleanGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean eligible() {
        if (keeper.isOnStrike()) return false;
        // Agent d'entretien : c'est son metier. Polyvalent : il depanne.
        return keeper.getJob() == 5 || keeper.getJob() == 0;
    }

    @Override
    public boolean canUse() {
        if (!eligible()) return false;
        if (--cooldown > 0) return false;
        cooldown = 60;
        if (!(keeper.level() instanceof ServerLevel level)) return false;
        litter = findNearestLitter(level);
        if (litter == null) {
            cooldown = 200; // rien a nettoyer : on ne rescanne pas trop souvent
            return false;
        }
        travelTicks = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (litter == null) return false;
        if (!(keeper.level() instanceof ServerLevel level)) return false;
        // Quelqu'un d'autre l'a peut-etre deja ramasse
        if (!level.getBlockState(litter).is(
                com.lex3d.ultimatezootaming.core.init.ModBlocks.LITTER.get())) return false;
        return travelTicks < 400;
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        litter = null;
        travelTicks = 0;
        keeper.setWorking(false);
        keeper.getNavigation().stop();
    }

    @Override
    public void tick() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.CLEANING);
        if (litter == null) return;
        if (!(keeper.level() instanceof ServerLevel level)) return;
        travelTicks++;

        double dist = keeper.blockPosition().distSqr(litter);
        if (dist > 2.5) {
            keeper.setWorking(false);
            keeper.getNavigation().moveTo(litter.getX() + 0.5, litter.getY(), litter.getZ() + 0.5, 1.0);
            keeper.getLookControl().setLookAt(
                    litter.getX() + 0.5, litter.getY() + 0.3, litter.getZ() + 0.5);
            // Chemin impossible : on abandonne ce dechet plutot que de tourner en rond
            if (keeper.getNavigation().isDone() && travelTicks > 100) stop();
            return;
        }

        // Sur place : ramassage
        keeper.getNavigation().stop();
        keeper.setWorking(true);
        LitterBlock.collect(level, litter, null);
        // MAITRISE (janitor) : chaque rang donne un recyclable supplementaire
        int extra = keeper.getJob() == 5 ? keeper.getMastery() : 0;
        for (int i = 0; i < extra; i++) {
            net.minecraft.world.level.block.Block.popResource(level, litter,
                    new net.minecraft.world.item.ItemStack(
                            com.lex3d.ultimatezootaming.core.init.ModItems.RECYCLABLE_WASTE.get()));
        }
        keeper.addXp(1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD,
                litter.getX() + 0.5, litter.getY() + 0.2, litter.getZ() + 0.5,
                3, 0.15, 0.05, 0.15, 0.01);
        litter = null;
        cooldown = 20; // il enchaine vite s'il y a d'autres detritus
    }

    /**
     * Cherche le detritus le plus proche. Scan PLAT uniquement (dy -2..+2) pour
     * ne jamais parcourir un volume complet.
     */
    private BlockPos findNearestLitter(ServerLevel level) {
        BlockPos base = keeper.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        var litterBlock = com.lex3d.ultimatezootaming.core.init.ModBlocks.LITTER.get();
        // PORTEE : la competence elargit le rayon de recherche
        int search = BASE_SEARCH + keeper.getRangeBonus();
        for (int dx = -search; dx <= search; dx += 2) {
            for (int dz = -search; dz <= search; dz += 2) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (!level.getBlockState(p).is(litterBlock)) continue;
                    double d = base.distSqr(p);
                    if (d < bestD) { bestD = d; best = p.immutable(); }
                }
            }
        }
        return best;
    }
}
