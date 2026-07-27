package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** L'Entree du zoo : arche a tourniquet — il tourne a chaque visiteur qui entre. */
public class ZooEntranceBlockEntity extends BlockEntity
        implements software.bernie.geckolib.animatable.GeoBlockEntity {

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache animCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public ZooEntranceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZOO_ENTRANCE.get(), pos, state);
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenLoop("animation.zoo_entrance.idle")))
                .triggerableAnim("turn", software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenPlay("animation.zoo_entrance.turn")));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
