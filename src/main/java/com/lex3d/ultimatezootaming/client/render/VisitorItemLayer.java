package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/** Affiche l'objet tenu (soda, popcorn, glace, barbe a papa, ballon) comme un
 *  VRAI item vanilla dans la main du visiteur, ancre au bone "right_item". */
public class VisitorItemLayer extends BlockAndItemGeoLayer<VisitorEntity> {

    public VisitorItemLayer(GeoRenderer<VisitorEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, VisitorEntity animatable) {
        return "right_item".equals(bone.getName()) ? animatable.getHeldStack() : null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, VisitorEntity animatable) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
