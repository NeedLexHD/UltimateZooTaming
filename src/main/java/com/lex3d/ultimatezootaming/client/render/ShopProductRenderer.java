package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.items.ShopProductItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class ShopProductRenderer extends GeoItemRenderer<ShopProductItem> {
    public ShopProductRenderer() { super(new ShopProductModel()); }
}
