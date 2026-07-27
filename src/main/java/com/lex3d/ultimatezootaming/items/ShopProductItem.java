package com.lex3d.ultimatezootaming.items;

import com.lex3d.ultimatezootaming.blocks.ShopBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Produit vendu en boutique : type de boutique + prix.
 *
 * Certains produits ont un MODELE 3D GeckoLib (casquette, badge). Les autres
 * restent en icone 2D classique : on ne paie le rendu anime que quand il
 * apporte quelque chose.
 */
public class ShopProductItem extends Item implements software.bernie.geckolib.animatable.GeoItem {

    private final ShopBlock.ShopType shopType;
    private final int price;
    /** Nom du modele GeckoLib, ou null pour un rendu 2D classique. */
    private final String modelName;

    private final software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache geoCache =
            software.bernie.geckolib.util.GeckoLibUtil.createInstanceCache(this);

    public ShopProductItem(ShopBlock.ShopType shopType, int price, Properties properties) {
        this(shopType, price, properties, null);
    }

    public ShopProductItem(ShopBlock.ShopType shopType, int price, Properties properties,
                           String modelName) {
        super(properties);
        this.shopType = shopType;
        this.price = price;
        this.modelName = modelName;
    }

    /** Null si ce produit n'a pas de modele 3D. */
    public String getModelName() { return modelName; }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        if (modelName == null) return; // produit 2D : rien a faire
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private com.lex3d.ultimatezootaming.client.render.ShopProductRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new com.lex3d.ultimatezootaming.client.render.ShopProductRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers) {
        if (modelName == null) return;
        controllers.add(new software.bernie.geckolib.core.animation.AnimationController<>(
                this, "idle", 0, state -> state.setAndContinue(
                        software.bernie.geckolib.core.animation.RawAnimation.begin()
                                .thenLoop("animation." + modelName + ".idle"))));
    }

    @Override
    public software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    public ShopBlock.ShopType getShopType() { return shopType; }

    public int getPrice() { return price; }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.product_price", price));
        tooltip.add(Component.translatable("tooltip.ultimatezootaming.product_shop",
                Component.translatable("shop.ultimatezootaming." + shopType.name().toLowerCase())));
    }
}
