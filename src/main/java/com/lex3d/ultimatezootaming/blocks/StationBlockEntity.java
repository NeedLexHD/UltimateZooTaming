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

/** BlockEntity GeckoLib des bornes d'interaction (photo / nourrissage / jet d'eau).
 *  Anime une pose idle en boucle + une anim "use" declenchee quand la borne
 *  s'active (clic droit ou visiteur) : le flash s'allume, la buse tire. */
public class StationBlockEntity extends BlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public StationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STATION.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Le registre en memoire est vide apres un redemarrage : chaque borne se
        // re-declare au chargement de son chunk.
        if (level != null && !level.isClientSide()) {
            InteractionStationBlock.register(level, worldPosition);
        }
    }

    /** Nom court pour les ressources : photo_spot / feed_station / water_jet. */
    public String stationKey() {
        if (getBlockState().getBlock() instanceof InteractionStationBlock st) {
            return switch (st.getKind()) {
                case PHOTO -> "photo_spot";
                case FEED -> "feed_station";
                case WATER -> "water_jet";
            };
        }
        return "water_jet";
    }

    /** Declenche l'animation "use" (flash / tir) cote client via GeckoLib. */
    public void triggerUse() {
        if (level != null && !level.isClientSide) {
            triggerAnim("main", "use");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        final String key = stationKey();
        controllers.add(new AnimationController<>(this, "main", 0, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation." + key + ".idle")))
                .triggerableAnim("use", RawAnimation.begin().thenPlay("animation." + key + ".use")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}
