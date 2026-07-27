package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.DecorBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class DecorRenderer extends GeoBlockRenderer<DecorBlockEntity> {
    public DecorRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new DecorModel());
    }
}
