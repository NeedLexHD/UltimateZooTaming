package com.lex3d.ultimatezootaming.ai;

import com.lex3d.ultimatezootaming.blocks.ShopBlock;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Tenir la caisse : le metier Vendeur (4) y va toujours, et un POLYVALENT (0)
 * sans enclos assigne prend automatiquement le poste d'une caisse libre — pose
 * une caisse, un employe libre s'en occupe. Un seul vendeur par caisse.
 */
public class KeeperShopGoal extends Goal {

    private static final double SEARCH = 128.0;
    /** Caisse -> vendeur en poste (un seul par caisse). */
    private static final java.util.Map<BlockPos, java.util.UUID> CLAIMS =
            new java.util.concurrent.ConcurrentHashMap<>();

    private final ZooKeeperEntity keeper;
    private BlockPos shop;
    private BlockPos standPos;
    private int cooldown;

    public KeeperShopGoal(ZooKeeperEntity keeper) {
        this.keeper = keeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean eligible() {
        if (keeper.isOnStrike()) return false;
        if (keeper.getJob() == 4) return true;
        // Polyvalent sans enclos : il s'adapte et tient une caisse libre
        return keeper.getJob() == 0 && keeper.getAssignedZone() == null;
    }

    /** La caisse est-elle deja tenue par un autre employe vivant ? */
    private boolean claimedByOther(BlockPos p) {
        java.util.UUID owner = CLAIMS.get(p);
        return owner != null && !owner.equals(keeper.getUUID());
    }

    @Override
    public boolean canUse() {
        if (!eligible()) return false;
        if (--cooldown > 0) return false;
        cooldown = 60;
        shop = ShopBlock.nearestShop(keeper.level(), keeper.blockPosition(), SEARCH);
        if (shop == null) {
            cooldown = 200;
            shop = ShopBlock.scanForShop(keeper.level(), keeper.blockPosition(), 24);
        }
        if (shop != null && claimedByOther(shop)) {
            // Cette caisse a deja son vendeur : un Vendeur de metier insiste ailleurs,
            // un polyvalent laisse tomber
            shop = null;
        }
        if (shop == null) return false;
        CLAIMS.put(shop, keeper.getUUID());
        // Se poste derriere le comptoir (cote oppose a la face avant)
        var state = keeper.level().getBlockState(shop);
        Direction facing = state.hasProperty(ShopBlock.FACING) ? state.getValue(ShopBlock.FACING) : Direction.NORTH;
        standPos = shop.relative(facing.getOpposite());
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return eligible() && shop != null && !claimedByOther(shop);
    }

    @Override
    public void tick() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.SELLING);
        if (standPos == null) return;
        double d = keeper.blockPosition().distSqr(standPos);
        if (d > 2.5) {
            if (keeper.getNavigation().isDone()) {
                keeper.getNavigation().moveTo(standPos.getX() + 0.5, standPos.getY(), standPos.getZ() + 0.5, 1.0);
            }
        } else {
            keeper.getNavigation().stop();
            // Regarde vers le comptoir (donc vers les clients)
            keeper.getLookControl().setLookAt(shop.getX() + 0.5, shop.getY() + 1.0, shop.getZ() + 0.5);
        }
    }

    @Override
    public void stop() {
        keeper.setTask(com.lex3d.ultimatezootaming.entities.KeeperTask.IDLE);
        if (shop != null) CLAIMS.remove(shop, keeper.getUUID());
        shop = null;
        standPos = null;
        keeper.getNavigation().stop();
    }
}
