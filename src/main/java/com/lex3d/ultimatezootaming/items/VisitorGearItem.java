package com.lex3d.ultimatezootaming.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * Objet tenu par les visiteurs, rendu en 3D par GeckoLib.
 *
 * Une seule classe pour tous : soda, popcorn, glace, barbe a papa, ballons,
 * jumelles, perche a selfie, photo. Le nom du modele est passe au constructeur,
 * ce qui evite treize classes quasi identiques.
 *
 * Le geo, l'animation et la texture se deduisent du nom :
 *   geo/gear/<nom>.geo.json
 *   animations/gear/<nom>.animation.json
 *   textures/item/gear/<nom>.png
 */
public class VisitorGearItem extends Item implements software.bernie.geckolib.animatable.GeoItem {

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    /** Nom du geo ET de l'animation. */
    private final String modelName;
    /**
     * Nom de la texture, separe du modele : les six ballons partagent la meme
     * geometrie mais ont chacun leur couleur.
     */
    private final String textureName;
    /** Animation de consommation (null si l'objet ne se mange pas). */
    private final UseAnim useAnim;

    public VisitorGearItem(Properties p, String modelName) {
        this(p, modelName, modelName, null);
    }

    public VisitorGearItem(Properties p, String modelName, UseAnim useAnim) {
        this(p, modelName, modelName, useAnim);
    }

    public VisitorGearItem(Properties p, String modelName, String textureName, UseAnim useAnim) {
        super(p);
        this.modelName = modelName;
        this.textureName = textureName;
        this.useAnim = useAnim;
    }

    public String getModelName() { return modelName; }
    public String getTextureName() { return textureName; }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (useAnim != null) return useAnim;
        return super.getUseAnimation(stack);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private com.lex3d.ultimatezootaming.client.render.VisitorGearRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new com.lex3d.ultimatezootaming.client.render.VisitorGearRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(
                this, "idle", 0, state -> state.setAndContinue(
                        software.bernie.geckolib.core.animation.RawAnimation.begin()
                                .thenLoop("animation." + modelName + ".idle"))));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
