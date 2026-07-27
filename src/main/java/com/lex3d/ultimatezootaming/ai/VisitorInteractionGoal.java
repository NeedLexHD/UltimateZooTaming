package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.InteractionStationBlock;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlock;
import com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity;
import com.lex3d.ultimatezootaming.capability.CapabilityHandler;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * Le visiteur va a une borne d'interaction (Photo / Nourrissage / Jet d'eau),
 * paie, une animation joue, et ca profite au visiteur ET (nourrissage/eau) a
 * l'animal le plus proche. Le vrai cote "parc a theme".
 */
public class VisitorInteractionGoal extends Goal {

    private static final double SEARCH = 20.0;
    private static final int COST = 2; // emeraudes payees par le visiteur

    private final VisitorEntity visitor;
    private BlockPos station;
    private InteractionStationBlock.Kind kind;
    private int cooldown;
    private int busyTicks;
    /** Ticks passes a essayer d'atteindre la borne (detection d'attente). */
    private int approachTicks;

    public VisitorInteractionGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.LOOK)); // le cerveau gere le deplacement
    }

    @Override
    public boolean canUse() {
        if (visitor.isLeaving()) return false;
        if (--cooldown > 0) return false;
        cooldown = 100 + visitor.getRandom().nextInt(200);
        // 1 chance sur 2 de vouloir interagir quand il croise une borne
        if (visitor.getRandom().nextBoolean()) return false;
        if (!(visitor.level() instanceof ServerLevel level)) return false;

        BlockPos found = null;
        double best = Double.MAX_VALUE;
        BlockPos origin = visitor.blockPosition();
        // REGISTRE des bornes : plus de scan de volume ici non plus.
        for (BlockPos p : InteractionStationBlock.getStationsIn(level)) {
            if (p.distSqr(origin) > SEARCH * SEARCH) continue;
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof InteractionStationBlock station) {
                double d = p.distSqr(origin);
                if (d < best) { best = d; found = p.immutable(); this.kind = station.getKind(); }
            }
        }
        if (found == null) return false;
        station = found;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return station != null && !visitor.isLeaving()
                && visitor.level().getBlockState(station).getBlock() instanceof InteractionStationBlock;
    }

    @Override
    public void start() {
        busyTicks = 0;
        approachTicks = 0;
        visitor.navigateVia(station.getX() + 0.5, station.getY(), station.getZ() + 0.5, 1.0);
    }

    @Override
    public void tick() {
        if (station == null) return;
        double dist = visitor.distanceToSqr(station.getX() + 0.5, station.getY(), station.getZ() + 0.5);
        if (dist > 4.0) {
            if (visitor.getNavigation().isDone()) {
                visitor.navigateVia(station.getX() + 0.5, station.getY(), station.getZ() + 0.5, 1.0);
            }
            // IMPATIENCE : quelqu'un occupe deja la borne et ca traine.
            // Le visiteur presse renonce plus vite que le contemplatif.
            approachTicks++;
            int limit = 220 + visitor.getPace() * 130;
            if (approachTicks > limit && visitor.level() instanceof ServerLevel isl) {
                isl.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        visitor.getX(), visitor.getEyeY() + 0.4, visitor.getZ(),
                        2, 0.15, 0.1, 0.15, 0.0);
                VisitorOpinion.say(isl, visitor, "crowded");
                station = null; // il passe a autre chose
            }
            return;
        }
        // Arrive a la borne : regarde-la, s'active
        visitor.getNavigation().stop();
        visitor.getLookControl().setLookAt(station.getX() + 0.5, station.getY() + 0.5, station.getZ() + 0.5);
        busyTicks++;
        if (!(visitor.level() instanceof ServerLevel level)) return;

        if (busyTicks == 1) {
            // Paiement : va a la Caisse du Zoo
            ZooVaultBlockEntity vault = ZooVaultBlock.anyVault(level);
            if (vault != null) vault.deposit(COST);
            com.lex3d.ultimatezootaming.saveddata.ZooLedger.get(level).addSales(COST);
        }
        // Effets selon le type, toutes les 10 ticks
        if (busyTicks % 10 == 0) {
            com.lex3d.ultimatezootaming.blocks.StationEffect.play(level, station);
            // Declenche l'animation de la borne (flash / buse qui tire)
            if (level.getBlockEntity(station)
                    instanceof com.lex3d.ultimatezootaming.blocks.StationBlockEntity sbe) {
                sbe.triggerUse();
            }
        }
        if (busyTicks > 60) { // ~3s d'interaction
            visitor.addJoy(kind == InteractionStationBlock.Kind.PHOTO ? 15 : 12);
            VisitorOpinion.say(level, visitor, opinionKey());
            // Souvenir de la borne photo : le visiteur sort son cliche et le
            // contemple une dizaine de secondes en repartant.
            if (kind == InteractionStationBlock.Kind.PHOTO) {
                visitor.admirePhoto(200); // ~10 s
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        visitor.getX(), visitor.getEyeY() + 0.2, visitor.getZ(),
                        5, 0.2, 0.2, 0.2, 0.01);
            }
            station = null;
        }
    }


    private String opinionKey() {
        return switch (kind) {
            case PHOTO -> "photo_fun";
            case FEED -> "feed_fun";
            case WATER -> "water_fun";
        };
    }

    @Override
    public void stop() {
        station = null;
        visitor.getNavigation().stop();
    }
}
