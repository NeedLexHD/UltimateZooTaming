package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import com.lex3d.ultimatezootaming.saveddata.ZooSavedData;
import com.lex3d.ultimatezootaming.zones.ZooZone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.ArrayList;
import java.util.List;

/**
 * Le visiteur choisit un enclos, va se poster devant (a l'exterieur, pres du
 * bord) et admire les animaux : plus l'enclos est beau, plus il est content —
 * et un visiteur content depense en boutique.
 */
public class VisitorViewGoal extends Goal {

    private static final int WATCH_TICKS = 200; // 10s devant l'enclos

    private final VisitorEntity visitor;
    private BlockPos target;
    private ZooZone zone;
    private int watching;
    private boolean signBonusGiven;
    private int travelTicks;
    private int cooldown;

    public VisitorViewGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (visitor.isLeaving()) return false;
        if (--cooldown > 0) return false;
        cooldown = 40;
        if (!(visitor.level() instanceof ServerLevel level)) return false;

        List<ZooZone> zones = new ArrayList<>(ZooSavedData.get(level).getAllZones());
        if (zones.isEmpty()) return false;
        // Un enclos au hasard, pas trop loin
        for (int tries = 0; tries < 4; tries++) {
            ZooZone z = zones.get(visitor.getRandom().nextInt(zones.size()));
            var bb = z.boundingBox();
            BlockPos center = BlockPos.containing((bb.minX + bb.maxX) / 2, bb.minY, (bb.minZ + bb.maxZ) / 2);
            if (visitor.blockPosition().distSqr(center) > 120 * 120) continue;
            // Position devant l'enclos : un bloc a l'exterieur du bord
            BlockPos spot = findSpotOutside(level, z, center);
            if (spot == null) continue;
            zone = z;
            target = spot;
            travelTicks = 0;
            return true;
        }
        return false;
    }

    /** Cherche un point marchable juste a l'exterieur de la zone. */
    private BlockPos findSpotOutside(ServerLevel level, ZooZone z, BlockPos center) {
        var bb = z.boundingBox();
        int y = (int) bb.minY;
        int[][] offsets = {
                {(int) bb.minX - 2, (int) center.getZ()}, {(int) bb.maxX + 2, (int) center.getZ()},
                {(int) center.getX(), (int) bb.minZ - 2}, {(int) center.getX(), (int) bb.maxZ + 2}
        };
        for (int[] o : offsets) {
            for (int dy = -2; dy <= 3; dy++) {
                BlockPos p = new BlockPos(o[0], y + dy, o[1]);
                if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                        && !level.getBlockState(p.below()).isAir()) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canContinueToUse() {
        return !visitor.isLeaving() && target != null && travelTicks < 600;
    }

    @Override
    public void tick() {
        if (target == null) return;
        double d = visitor.blockPosition().distSqr(target);
        if (d > 4.0) {
            travelTicks++;
            if (visitor.getNavigation().isDone()) {
                visitor.navigateVia(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            }
            return;
        }
        // Arrive : il regarde les animaux
        visitor.getNavigation().stop();
        if (zone != null && visitor.level() instanceof ServerLevel level) {
            // Panneau educatif pres de l'enclos : le visiteur apprend, +joie (1x/visite)
            if (!signBonusGiven && com.lex3d.ultimatezootaming.blocks.ZooSignBlock
                    .anySignNear(level, visitor.blockPosition(), 8)) {
                signBonusGiven = true;
                visitor.addJoy(10);
                com.lex3d.ultimatezootaming.ai.VisitorOpinion.say(level, visitor, "learned");
            }
            List<Animal> animals = level.getEntitiesOfClass(Animal.class, zone.boundingBox(),
                    a -> a.isAlive() && zone.contains(a.blockPosition()));
            if (!animals.isEmpty()) {
                Animal a = animals.get(0);
                visitor.getLookControl().setLookAt(a.getX(), a.getEyeY(), a.getZ());
                if (watching % 40 == 0) {
                    // Joie selon le bien-etre moyen constate
                    int sum = 0, n = 0;
                    for (Animal an : animals) {
                        var cap = an.getCapability(
                                com.lex3d.ultimatezootaming.capability.CapabilityHandler.TAMING_DATA)
                                .resolve().orElse(null);
                        if (cap != null && cap.isTamed()) { sum += cap.getSatisfaction(); n++; }
                    }
                    if (n > 0) {
                        int avg = sum / n;
                        visitor.addJoy(avg >= 75 ? 12 : avg >= 40 ? 6 : 2);
                        if (avg < 30 && visitor.getRandom().nextInt(4) == 0) {
                            VisitorOpinion.say(level, visitor, "sad_animals");
                        } else if (avg >= 85 && visitor.getRandom().nextInt(8) == 0) {
                            VisitorOpinion.say(level, visitor, "happy_animals");
                        }
                        if (avg >= 75 && visitor.getRandom().nextInt(3) == 0) {
                            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                                    visitor.getX(), visitor.getEyeY() + 0.4, visitor.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
                        }
                    }
                }
            }
        }
        if (++watching > WATCH_TICKS) {
            watching = 0;
            target = null;
            cooldown = 100;
        }
    }

    @Override
    public void stop() {
        signBonusGiven = false;
        target = null;
        zone = null;
        watching = 0;
        visitor.getNavigation().stop();
    }
}
