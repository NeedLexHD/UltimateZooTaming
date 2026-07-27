package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.ZooVaultBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class ZooVaultRenderer extends GeoBlockRenderer<ZooVaultBlockEntity> {
    public ZooVaultRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new ZooVaultModel());
    }
}
