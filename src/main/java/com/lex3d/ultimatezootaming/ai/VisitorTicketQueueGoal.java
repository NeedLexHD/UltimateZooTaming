package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.TicketBoothBlock;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/**
 * IA de file d'attente au guichet : le visiteur va au guichet le plus proche,
 * fait la queue (attend qu'il y ait moins de 3 autres devant), "achete" son
 * billet (2 sec devant), puis marque hasTicket=true et laisse la place.
 * Actif SEULEMENT si hasTicket=false et qu'il existe des guichets dans le monde.
 */
public class VisitorTicketQueueGoal extends Goal {

    private final VisitorEntity visitor;
    private BlockPos targetBooth;
    /** ticks passes devant le guichet (pour simuler l'achat). */
    private int waitTicks = 0;
    /** Ticks passes a faire la queue derriere quelqu'un. */
    private int queueTicks = 0;
    /** ticks totaux (au cas ou on reste bloque, on abandonne apres 30s). */
    private int totalTicks = 0;

    public VisitorTicketQueueGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (visitor.hasTicket()) return false;
        if (visitor.isLeaving()) return false;
        if (!(visitor.level() instanceof ServerLevel sl)) return false;
        var booths = TicketBoothBlock.getBoothsIn(sl);
        return !booths.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && totalTicks < 600; // abandon apres 30s
    }

    @Override
    public void start() {
        // Choisit le guichet le plus proche
        if (!(visitor.level() instanceof ServerLevel sl)) return;
        var booths = TicketBoothBlock.getBoothsIn(sl);
        BlockPos nearest = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos b : booths) {
            double d = b.distSqr(visitor.blockPosition());
            if (d < bd) { bd = d; nearest = b; }
        }
        this.targetBooth = nearest;
        this.waitTicks = 0;
        this.queueTicks = 0;
        this.totalTicks = 0;
    }

    @Override
    public void stop() {
        this.targetBooth = null;
        this.waitTicks = 0;
        this.totalTicks = 0;
    }

    @Override
    public void tick() {
        totalTicks++;
        if (targetBooth == null) return;
        if (!(visitor.level() instanceof ServerLevel sl)) return;

        // Distance au guichet
        double dx = visitor.getX() - (targetBooth.getX() + 0.5);
        double dz = visitor.getZ() - (targetBooth.getZ() + 0.5);
        double dist2 = dx * dx + dz * dz;

        if (dist2 > 3.0 * 3.0) {
            // Marche vers le guichet, mais en gardant une distance de 1 bloc devant
            // (approche le devant du guichet, pas le bloc lui-meme).
            var facing = sl.getBlockState(targetBooth).getValue(TicketBoothBlock.FACING);
            BlockPos frontOfBooth = targetBooth.relative(facing);
            visitor.getNavigation().moveTo(frontOfBooth.getX() + 0.5,
                    frontOfBooth.getY(), frontOfBooth.getZ() + 0.5, 0.75);
        } else {
            // Assez proche : verifier si un autre visiteur est deja en train d'acheter
            // (fait la queue). On compte les visiteurs proches du guichet.
            AABB queueBox = new AABB(targetBooth).inflate(2.5);
            long othersHere = sl.getEntitiesOfClass(VisitorEntity.class, queueBox,
                    v -> v != visitor && !v.hasTicket()
                            && v.distanceToSqr(targetBooth.getX() + 0.5, targetBooth.getY(),
                                targetBooth.getZ() + 0.5) < dist2).size();
            if (othersHere == 0) {
                // Aucun devant : achete son billet
                visitor.getNavigation().stop();
                waitTicks++;
                if (waitTicks == 1) {
                    // Petit son de "ding" caisse
                    sl.playSound(null, targetBooth, net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),
                            net.minecraft.sounds.SoundSource.NEUTRAL, 0.4f, 1.3f);
                }
                if (waitTicks >= 40) { // 2 secondes d'achat
                    // Ticket obtenu
                    visitor.setTicket(true);
                    // Effet visuel : particules dorees
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                            visitor.getX(), visitor.getEyeY(), visitor.getZ(),
                            5, 0.3, 0.3, 0.3, 0.02);
                }
            } else {
                // Fait la queue : se met un peu en retrait, marche lentement
                queueTicks++;
                // IMPATIENCE : au-dela d'un certain temps, il renonce a entrer.
                // Un visiteur presse craque plus vite qu'un contemplatif.
                int limit = 300 + visitor.getPace() * 150;
                if (queueTicks > limit) {
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                            visitor.getX(), visitor.getEyeY() + 0.4, visitor.getZ(),
                            2, 0.15, 0.1, 0.15, 0.0);
                    VisitorOpinion.say(sl, visitor, "crowded");
                    visitor.setLeaving(true); // il repart sans avoir visite
                    return;
                }
                visitor.getNavigation().moveTo(
                        targetBooth.getX() + 0.5 + visitor.getRandom().nextDouble() * 2 - 1,
                        targetBooth.getY(),
                        targetBooth.getZ() + 0.5 + visitor.getRandom().nextDouble() * 2 - 1,
                        0.5);
            }
        }
    }
}
