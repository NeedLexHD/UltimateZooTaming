package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Tresorerie du zoo. */
public class ZooVaultBlockEntity extends BlockEntity
        implements software.bernie.geckolib.animatable.GeoBlockEntity {

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache animCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenLoop("animation.zoo_vault.idle")))
                .triggerableAnim("deposit", software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenPlay("animation.zoo_vault.deposit")));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Re-enregistre le coffre au chargement du chunk (le registre en memoire
        // est vide apres un redemarrage) -> evite les greves injustifiees.
        if (level != null && !level.isClientSide()) {
            ZooVaultBlock.register(level, worldPosition);
        }
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }


    private int balance = 0;

    public ZooVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZOO_VAULT.get(), pos, state);
    }

    public int getBalance() { return balance; }

    public void deposit(int amount) {
        if (amount > 0) triggerAnim("main", "deposit"); // la molette tourne !
        balance += amount;
        setChanged();
    }

    /** Retire si possible, retourne false si fonds insuffisants. */
    public boolean withdraw(int amount) {
        if (balance < amount) return false;
        balance -= amount;
        setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Balance", balance);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        balance = tag.getInt("Balance");
    }
}
