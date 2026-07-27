package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.IncubatorBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Rendu anime de l'incubateur : dome qui pulse pendant la couvaison. */
public class IncubatorRenderer extends GeoBlockRenderer<IncubatorBlockEntity> {
    public IncubatorRenderer() { super(new IncubatorModel()); }
}
