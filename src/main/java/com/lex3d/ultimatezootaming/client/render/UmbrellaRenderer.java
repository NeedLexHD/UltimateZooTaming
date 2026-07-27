package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.items.UmbrellaItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class UmbrellaRenderer extends GeoItemRenderer<UmbrellaItem> {
    public UmbrellaRenderer() { super(new UmbrellaModel()); }
}
