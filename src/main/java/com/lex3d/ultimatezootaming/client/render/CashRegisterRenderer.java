package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.blocks.ShopBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class CashRegisterRenderer extends GeoBlockRenderer<ShopBlockEntity> {
    public CashRegisterRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new CashRegisterModel());
    }
}
