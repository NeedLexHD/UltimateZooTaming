package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.items.WhistleItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class WhistleRenderer extends GeoItemRenderer<WhistleItem> {
    public WhistleRenderer() {
        super(new WhistleModel());
    }
}
