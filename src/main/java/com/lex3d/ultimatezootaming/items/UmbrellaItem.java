package com.lex3d.ultimatezootaming.items;

import net.minecraft.world.item.Item;

/**
 * Parapluie : item GeckoLib anime (leger balancement quand on le tient).
 * Deux variantes existent : normal (UMBRELLA) et avec oreilles kawaii (KAWAII_UMBRELLA).
 * Les visiteurs peuvent en tenir sous la pluie (via VisitorItemLayer).
 */
public class UmbrellaItem extends Item implements software.bernie.geckolib.animatable.GeoItem {

    /** true = variante "oreilles" (souvenir de boutique), false = normal. */
    private final boolean hasEars;

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public UmbrellaItem(Properties p, boolean hasEars) {
        super(p);
        this.hasEars = hasEars;
    }

    public boolean hasEars() { return hasEars; }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private com.lex3d.ultimatezootaming.client.render.UmbrellaRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new com.lex3d.ultimatezootaming.client.render.UmbrellaRenderer();
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(this, "idle", 0, state ->
                state.setAndContinue(software.bernie.geckolib.core.animation.RawAnimation.begin().thenLoop("animation.umbrella.idle"))));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
