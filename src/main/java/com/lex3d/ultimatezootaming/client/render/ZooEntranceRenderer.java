package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.ZooEntranceBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ZooEntranceRenderer extends GeoBlockRenderer<ZooEntranceBlockEntity> {
    public ZooEntranceRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new ZooEntranceModel());
    }
}
