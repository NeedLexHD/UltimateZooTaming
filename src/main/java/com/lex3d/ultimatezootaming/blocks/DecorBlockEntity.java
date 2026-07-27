package com.lex3d.ultimatezootaming.blocks;

import com.lex3d.ultimatezootaming.core.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** BE partage des blocs deco GeckoLib : panneau, banc, poubelle. */
public class DecorBlockEntity extends BlockEntity
        implements software.bernie.geckolib.animatable.GeoBlockEntity {

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache animCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public DecorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECOR.get(), pos, state);
    }

    /** Nom court du bloc pour les ressources : zoo_sign / zoo_bench / zoo_bin. */
    public String decorKey() {
        var block = getBlockState().getBlock();
        if (block instanceof ZooSignBlock) return "zoo_sign";
        if (block instanceof ZooAmenityBlock a && a.getKind() == ZooAmenityBlock.Kind.BIN) return "zoo_bin";
        return "zoo_bench";
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        final String key = decorKey();
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenLoop("animation." + key + ".idle")))
                .triggerableAnim("use", software.bernie.geckolib.core.animation.RawAnimation.begin()
                        .thenPlay("animation." + key + ".use")));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
