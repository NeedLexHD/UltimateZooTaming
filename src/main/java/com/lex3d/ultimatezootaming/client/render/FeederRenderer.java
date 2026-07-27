package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.FeederBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class FeederRenderer extends GeoBlockRenderer<FeederBlockEntity> {
    public FeederRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new FeederModel());
    }
}
