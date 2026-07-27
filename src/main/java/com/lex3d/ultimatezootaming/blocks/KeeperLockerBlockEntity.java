package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class KeeperLockerBlockEntity extends BlockEntity
        implements GeoBlockEntity, net.minecraft.world.Container {

    /** Stock de service du secteur : fourrage + remedes, ravitaille les soigneurs. */
    private final net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> items =
            net.minecraft.core.NonNullList.withSize(9, net.minecraft.world.item.ItemStack.EMPTY);

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(net.minecraft.world.item.ItemStack::isEmpty); }
    @Override public net.minecraft.world.item.ItemStack getItem(int slot) { return items.get(slot); }
    @Override public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
        var r = net.minecraft.world.ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) setChanged();
        return r;
    }
    @Override public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        return net.minecraft.world.ContainerHelper.takeItem(items, slot);
    }
    @Override public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        items.set(slot, stack); setChanged();
    }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
    @Override public void clearContent() { items.clear(); }

    /** Insere un stack (fusion), retourne ce qui n'a pas pu entrer. */
    public net.minecraft.world.item.ItemStack insert(net.minecraft.world.item.ItemStack incoming) {
        for (int i = 0; i < items.size() && !incoming.isEmpty(); i++) {
            var slot = items.get(i);
            if (slot.isEmpty()) {
                items.set(i, incoming.copy());
                incoming.setCount(0);
            } else if (net.minecraft.world.item.ItemStack.isSameItemSameTags(slot, incoming)) {
                int move = Math.min(incoming.getCount(), slot.getMaxStackSize() - slot.getCount());
                slot.grow(move); incoming.shrink(move);
            }
        }
        setChanged();
        return incoming;
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, items);
    }

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public KeeperLockerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KEEPER_LOCKER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Re-enregistre le vestiaire au chargement du chunk : sinon, apres un
        // redemarrage, le registre en memoire est vide et les nourrisseurs ne
        // trouvent plus leur stock.
        if (level != null && !level.isClientSide()) {
            com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock.register(level, worldPosition);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.keeper_locker.idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
