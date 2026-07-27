package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.ShopBlock;
import com.lex3d.ultimatezootaming.blocks.ShopBlockEntity;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * REAPPROVISIONNEMENT DE BOUTIQUE : quand la caisse dont il s'occupe manque de
 * stock, le vendeur va chercher des articles dans un conteneur voisin (coffre,
 * baril, ou conteneur d'un autre mod) situe a 5 blocs maximum de la caisse,
 * puis revient les deposer.
 *
 * Prioritaire sur le fait de tenir la caisse : une boutique vide ne sert a rien.
 */
public class KeeperRestockGoal extends Goal {

    /** Sous quel seuil de stock on declenche un reapprovisionnement. */
    private static final int LOW_STOCK = 8;
    private static final double SHOP_SEARCH = 64.0;

    private final ZooKeeperEntity keeper;

    private BlockPos shopPos;
    private BlockPos chestPos;
    /** 0 = aller au coffre, 1 = revenir a la caisse. */
    private int phase;
    private int cooldown;
    private int travelTicks;

    public KeeperRestockGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Seuls les vendeurs (et les polyvalents sans enclos) approvisionnent. */
    private boolean eligible() {
        if (keeper.isOnStrike()) return false;
        if (keeper.getJob() == 4) return true;
        return keeper.getJob() == 0 && keeper.getAssignedZone() == null;
    }

    @Override
    public boolean canUse() {
        if (!eligible()) return false;
        if (--cooldown > 0) return false;
        cooldown = 100; // on ne verifie pas a chaque tick
        if (!(keeper.level() instanceof ServerLevel level)) return false;

        // La caisse la plus proche qui manque de stock ET qui a un coffre voisin
        BlockPos shop = ShopBlock.nearestShop(level, keeper.blockPosition(), SHOP_SEARCH);
        if (shop == null) return false;
        if (!(level.getBlockEntity(shop) instanceof ShopBlockEntity be)) return false;
        if (be.countStock() > LOW_STOCK) return false; // encore assez de marchandise
        if (!be.hasRoom()) return false;               // caisse pleine

        BlockPos chest = be.findSupplyContainer();
        if (chest == null) return false;               // rien pour la regarnir

        this.shopPos = shop;
        this.chestPos = chest;
        this.phase = 0;
        this.travelTicks = 0;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (shopPos == null || chestPos == null) return false;
        if (!(keeper.level() instanceof ServerLevel level)) return false;
        // La caisse ou le coffre a disparu entre-temps
        if (!(level.getBlockEntity(shopPos) instanceof ShopBlockEntity)) return false;
        if (!(level.getBlockEntity(chestPos) instanceof Container)) return false;
        return travelTicks < 600; // securite : 30 s maximum par voyage
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        shopPos = null;
        chestPos = null;
        phase = 0;
        travelTicks = 0;
        keeper.setWorking(false);
        keeper.getNavigation().stop();
    }

    @Override
    public void tick() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.RESTOCK);
        if (!(keeper.level() instanceof ServerLevel level)) return;
        travelTicks++;
        BlockPos target = (phase == 0) ? chestPos : shopPos;
        double dist = keeper.blockPosition().distSqr(target);

        if (dist > 4.0) {
            // En route
            keeper.setWorking(false);
            keeper.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
            keeper.getLookControl().setLookAt(
                    target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
            // Secours : chemin impossible (comptoir enclave, denivele)
            if (keeper.getNavigation().isDone() && travelTicks > 120) {
                keeper.moveTo(target.getX() + 0.5, target.getY() + 1, target.getZ() + 0.5,
                        keeper.getYRot(), keeper.getXRot());
                keeper.getNavigation().stop();
            }
            return;
        }

        // Arrive a destination
        keeper.getNavigation().stop();
        keeper.setWorking(true);

        if (phase == 0) {
            // Au coffre : on prend de la marchandise vendable
            if (!(level.getBlockEntity(chestPos) instanceof Container cont)) { stop(); return; }
            if (!(level.getBlockEntity(shopPos) instanceof ShopBlockEntity be)) { stop(); return; }
            boolean took = be.restockFrom(cont);
            level.playSound(null, chestPos,
                    net.minecraft.sounds.SoundEvents.BARREL_OPEN,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.4f, 1.1f);
            if (!took) { stop(); return; } // plus rien de vendable : inutile d'insister
            phase = 1;
            travelTicks = 0;
        } else {
            // De retour a la caisse : effet visuel de rangement, puis fin de cycle
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    shopPos.getX() + 0.5, shopPos.getY() + 1.1, shopPos.getZ() + 0.5,
                    4, 0.3, 0.2, 0.3, 0.01);
            level.playSound(null, shopPos,
                    net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.2f);
            keeper.addXp(1); // ravitailler compte comme du travail
            stop();
        }
    }
}
