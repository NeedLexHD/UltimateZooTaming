package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Fin de visite : le visiteur retourne a l'entree et disparait. */
public class VisitorLeaveGoal extends Goal {

    private final VisitorEntity visitor;
    private int travelTicks;

    public VisitorLeaveGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return visitor.isLeaving();
    }

    @Override
    public void tick() {
        // Avant de sortir : jeter son dechet a la poubelle (sinon il rale)
        if (visitor.hasLitter() && visitor.level() instanceof net.minecraft.server.level.ServerLevel level) {
            BlockPos bin = com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.nearest(
                    com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock.Kind.BIN, level, visitor.blockPosition(), 32);
            if (bin != null) {
                if (visitor.blockPosition().distSqr(bin) > 4.0) {
                    if (visitor.getNavigation().isDone()) {
                        visitor.navigateVia(bin.getX() + 0.5, bin.getY(), bin.getZ() + 0.5, 1.0);
                    }
                    return;
                }
                visitor.setLitter(false);
                if (level.getBlockEntity(bin) instanceof
                        com.lex3d.ultimatezootaming.blocks.DecorBlockEntity dbe) {
                    dbe.triggerAnim("main", "use"); // le couvercle bat
                }
                level.playSound(null, bin, net.minecraft.sounds.SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.4f, 0.9f);
            } else {
                // Pas de poubelle : il garde son dechet et donne son avis
                VisitorOpinion.say(level, visitor, "no_bin");
                visitor.setLitter(false);
                visitor.spendJoy(5);
            }
        }
        BlockPos exit = visitor.getEntrance();
        if (exit == null) { visitor.discard(); return; }
        double d = visitor.blockPosition().distSqr(exit);
        if (d < 4.0 || ++travelTicks > 1200) {
            if (visitor.level() instanceof net.minecraft.server.level.ServerLevel level) {
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                        visitor.getX(), visitor.getY() + 0.5, visitor.getZ(), 5, 0.2, 0.3, 0.2, 0.01);
            }
            visitor.discard();
            return;
        }
        if (visitor.getNavigation().isDone()) {
            visitor.navigateVia(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5, 1.1);
        }
    }
}
