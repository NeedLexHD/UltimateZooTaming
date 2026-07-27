package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.StationBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class StationRenderer extends GeoBlockRenderer<StationBlockEntity> {
    public StationRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new StationModel());
    }
}
