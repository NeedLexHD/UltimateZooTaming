package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.items.VisitorGearItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/** Un seul renderer pour tous les objets de visiteur. */
public class VisitorGearRenderer extends GeoItemRenderer<VisitorGearItem> {
    public VisitorGearRenderer() { super(new VisitorGearModel()); }
}
