package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.KeeperLockerBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class KeeperLockerRenderer extends GeoBlockRenderer<KeeperLockerBlockEntity> {
    public KeeperLockerRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new KeeperLockerModel());
    }
}
