package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Affiche l'OUTIL DE METIER dans la main de l'employe : fiole pour le
 * veterinaire, fourrage pour le nourrisseur, sifflet pour le garde, billets
 * pour le vendeur, sac pour l'agent d'entretien.
 * Ancre au bone "right_item" du modele du soigneur.
 */
public class KeeperToolLayer extends BlockAndItemGeoLayer<ZooKeeperEntity> {

    public KeeperToolLayer(GeoRenderer<ZooKeeperEntity> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, ZooKeeperEntity animatable) {
        return "right_item".equals(bone.getName()) ? animatable.getToolStack() : null;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack,
                                                          ZooKeeperEntity animatable) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }
}
