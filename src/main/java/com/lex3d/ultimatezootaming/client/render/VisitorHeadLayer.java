package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Coiffe le visiteur : la casquette du zoo achetee en boutique souvenir se pose
 * sur sa tete et suit ses mouvements. Ancre au bone "head_item".
 */
public class VisitorHeadLayer extends BlockAndItemGeoLayer<VisitorEntity> {

    public VisitorHeadLayer(GeoRenderer<VisitorEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, VisitorEntity animatable) {
        return "head_item".equals(bone.getName()) ? animatable.getHeadStack() : null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                          VisitorEntity animatable) {
        return ItemDisplayContext.HEAD;
    }
}
