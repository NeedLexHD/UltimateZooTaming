package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.ShopBlock;
import com.lex3d.ultimatezootaming.blocks.ShopBlockEntity;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Un visiteur content (joie >= 30) va faire un achat dans une boutique en stock.
 * L'emeraude part dans la Caisse du Zoo (gere par ShopBlockEntity.sellOne()).
 */
public class VisitorShopGoal extends Goal {

    private final VisitorEntity visitor;
    private BlockPos shop;
    private int travelTicks;
    private int cooldown;
    /** Ticks passes a patienter devant la caisse. */
    private int queueTicks;

    public VisitorShopGoal(VisitorEntity visitor) {
        this.visitor = visitor;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (visitor.isLeaving() || visitor.getJoy() < 30) return false;
        if (--cooldown > 0) return false;
        cooldown = 100;
        if (!(visitor.level() instanceof ServerLevel level)) return false;

        BlockPos p = ShopBlock.nearestShop(level, visitor.blockPosition(), 80);
        if (p == null) return false;
        if (!(level.getBlockEntity(p) instanceof ShopBlockEntity shopBe) || !shopBe.hasStock()) return false;
        shop = p;
        travelTicks = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !visitor.isLeaving() && shop != null && travelTicks < 600;
    }

    @Override
    public void tick() {
        if (shop == null) return;
        double d = visitor.blockPosition().distSqr(shop);
        if (d > 4.0) {
            travelTicks++;
            if (visitor.getNavigation().isDone()) {
                visitor.navigateVia(shop.getX() + 0.5, shop.getY(), shop.getZ() + 0.5, 1.0);
            }
            return;
        }
        visitor.getNavigation().stop();
        visitor.getLookControl().setLookAt(shop.getX() + 0.5, shop.getY() + 1.0, shop.getZ() + 0.5);

        // IMPATIENCE : combien de clients sont deja devant lui ?
        queueTicks++;
        if (visitor.level() instanceof ServerLevel ql) {
            int ahead = ql.getEntitiesOfClass(com.lex3d.ultimatezootaming.entities.VisitorEntity.class,
                    new net.minecraft.world.phys.AABB(shop).inflate(3.0),
                    v -> v != visitor && v.blockPosition().distSqr(shop) < d).size();
            // Un visiteur presse (pace 0) patiente moins, un contemplatif davantage
            int limit = 200 + visitor.getPace() * 120;
            if (ahead > 0 && queueTicks > limit) {
                com.lex3d.ultimatezootaming.ai.VisitorOpinion.say(ql, visitor, "crowded");
                ql.sendParticles(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        visitor.getX(), visitor.getEyeY() + 0.4, visitor.getZ(), 2, 0.15, 0.1, 0.15, 0.0);
                shop = null;
                cooldown = 600; // il boude cette boutique un moment
                queueTicks = 0;
                return;
            }
        }

        if (visitor.level() instanceof ServerLevel level
                && level.getBlockEntity(shop) instanceof ShopBlockEntity shopBe) {
            if (!shopBe.hasVendor()) {
                com.lex3d.ultimatezootaming.ai.VisitorOpinion.say(level, visitor, "no_vendor");
                shop = null;
                cooldown = 400;
                return;
            }
            int paid = shopBe.sellOne();
            if (paid > 0) {
                visitor.spendJoy(30);
                level.playSound(null, shop, net.minecraft.sounds.SoundEvents.VILLAGER_YES,
                        net.minecraft.sounds.SoundSource.NEUTRAL, 0.5f, 1.2f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        visitor.getX(), visitor.getEyeY(), visitor.getZ(), 4, 0.3, 0.3, 0.3, 0.0);

                var type = shopBe.getShopTypeEnum();

                // SOUVENIR PORTE : une casquette achetee se met sur la tete
                if (type == com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.SOUVENIR
                        && !visitor.hasCap() && visitor.getRandom().nextInt(3) == 0) {
                    visitor.setCap(true);
                }

                // --- RAPPORT QUALITE / PRIX ---
                // Le visiteur juge ce qu'il vient d'acheter : un article qui comble
                // un vrai besoin (boisson quand il a soif, repas quand il a faim)
                // justifie son prix. Un article de plaisir pur est juge au hasard,
                // parce que la valeur percue depend de l'envie du moment.
                boolean satisfiesNeed =
                        (type == com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.DRINK
                                && visitor.getThirst() > 50)
                     || ((type == com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.MEAL
                          || type == com.lex3d.ultimatezootaming.blocks.ShopBlock.ShopType.ICECREAM)
                                && visitor.getHunger() > 50);
                // Seuil de tolerance : plus l'article rend service, plus on accepte
                // de payer. Sans besoin reel, la barre est bien plus basse.
                int tolerance = satisfiesNeed ? 12 : 5;
                if (paid > tolerance) {
                    com.lex3d.ultimatezootaming.ai.VisitorOpinion.say(level, visitor, "too_expensive");
                } else if (satisfiesNeed && paid <= tolerance / 2) {
                    // Vraiment bien place : le visiteur le fait savoir
                    visitor.addJoy(10); // bonne affaire : le visiteur repart content
                }
            }
        }
        shop = null;
        queueTicks = 0;
        cooldown = 400; // il ne rachete pas tout de suite
    }

    @Override
    public void stop() {
        shop = null;
        visitor.getNavigation().stop();
    }
}
