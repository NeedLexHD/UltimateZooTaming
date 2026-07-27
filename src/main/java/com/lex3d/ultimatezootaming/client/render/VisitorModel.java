package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.entities.VisitorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Le visiteur reutilise le squelette du soigneur, avec des SKINS LOCAUX.
 *  Les skins sont dans textures/entity/visitor/visitor_1.png ... visitor_N.png
 *  (format skin Minecraft 64x64). Tu peux en ajouter/remplacer librement :
 *  augmente juste SKIN_COUNT dans VisitorEntity pour en charger davantage. */
public class VisitorModel extends GeoModel<VisitorEntity> {

    @Override
    public ResourceLocation getModelResource(VisitorEntity e) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/visitor.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(VisitorEntity e) {
        // DEUX BANQUES DE SKINS separees :
        //   textures/entity/visitor/adult/visitor_1.png .. visitor_60.png
        //   textures/entity/visitor/child/visitor_1.png .. visitor_20.png
        // Un enfant ne prend jamais un skin d'adulte et inversement.
        int n = e.getSkin() + 1; // getSkin() renvoie 0..SKIN_COUNT-1
        if (e.isBaby()) {
            int c = ((n - 1) % VisitorEntity.CHILD_SKIN_COUNT) + 1;
            return new ResourceLocation(UltimateZooTame.MODID,
                    "textures/entity/visitor/child/visitor_" + c + ".png");
        }
        return new ResourceLocation(UltimateZooTame.MODID,
                "textures/entity/visitor/adult/visitor_" + n + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(VisitorEntity e) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/zookeeper.animation.json");
    }
}
