package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.ShopBlock;
import com.lex3d.ultimatezootaming.blocks.ShopBlockEntity;
import com.lex3d.ultimatezootaming.blocks.ZooAmenityBlock;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Le visiteur repond a son besoin le plus urgent : manger (stand repas/glaces),
 * boire (stand boissons), se reposer (banc). S'il ne trouve rien, il rale (avis).
 */
public class VisitorNeedsGoal extends Goal {

    private final VisitorEntity visitor;
    private BlockPos target;
    private int need;         // 1 faim, 2 soif, 3 fatigue
    private int travelTicks;
    private int restTicks;
    private int cooldown;

    public VisitorNeedsGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (visitor.isLeaving()) return false;
        if (--cooldown > 0) return false;
        cooldown = 60;
        need = visitor.urgentNeed();
        if (need == 0) return false;
        if (!(visitor.level() instanceof ServerLevel level)) return false;

        if (need == 3) {
            target = ZooAmenityBlock.nearest(ZooAmenityBlock.Kind.BENCH, level, visitor.blockPosition(), 60);
            if (target == null) { VisitorOpinion.say(level, visitor, "no_bench"); cooldown = 600; return false; }
        } else {
            target = findShop(level, need);
            if (target == null) {
                VisitorOpinion.say(level, visitor, need == 1 ? "no_food" : "no_drink");
                cooldown = 600;
                return false;
            }
        }
        travelTicks = 0;
        restTicks = 0;
        return true;
    }

    /** Stand correspondant au besoin, avec du stock. */
    private BlockPos findShop(ServerLevel level, int need) {
        BlockPos best = null;
        double bd = 70 * 70;
        // REGISTRE des caisses : ce scan balayait 111 537 blocs a chaque
        // verification de besoin. On parcourt desormais une poignee de positions.
        for (BlockPos p : ShopBlock.allShops(level)) {
            if (p.distSqr(visitor.blockPosition()) > 70 * 70) continue;
            if (!(level.getBlockEntity(p) instanceof ShopBlockEntity be) || !be.hasStock()) continue;
            ShopBlock.ShopType t = be.getShopTypeEnum();
            boolean match = need == 1
                    ? (t == ShopBlock.ShopType.MEAL || t == ShopBlock.ShopType.ICECREAM)
                    : t == ShopBlock.ShopType.DRINK;
            if (!match) continue;
            double d = visitor.blockPosition().distSqr(p);
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        return best;
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
        visitor.getNavigation().stop();
        visitor.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
        if (!(visitor.level() instanceof ServerLevel level)) return;

        if (need == 3) {
            // Sur le banc : il souffle un peu
            if (++restTicks > 160) {
                visitor.rest();
                visitor.addJoy(6);
                target = null;
                cooldown = 200;
            }
            return;
        }
        // Achat qui satisfait le besoin — s'il y a quelqu'un a la caisse !
        if (level.getBlockEntity(target) instanceof ShopBlockEntity be) {
            if (!be.hasVendor()) {
                VisitorOpinion.say(level, visitor, "no_vendor");
                target = null;
                cooldown = 300;
                return;
            }
            int paid = be.sellOne();
            if (paid > 0) {
                if (need == 1) visitor.satisfyHunger(); else visitor.satisfyThirst();
                visitor.addJoy(8);
                visitor.setLitter(true); // il a un emballage a jeter
                level.playSound(null, target, net.minecraft.sounds.SoundEvents.GENERIC_EAT,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.1f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        visitor.getX(), visitor.getEyeY(), visitor.getZ(), 3, 0.2, 0.2, 0.2, 0.0);
            }
        }
        target = null;
        cooldown = 100;
    }

    @Override
    public void stop() {
        target = null;
        restTicks = 0;
        visitor.getNavigation().stop();
    }
}
