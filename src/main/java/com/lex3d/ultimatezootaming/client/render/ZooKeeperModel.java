package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.entities.ZooKeeperEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ZooKeeperModel extends GeoModel<ZooKeeperEntity> {

    @Override
    public ResourceLocation getModelResource(ZooKeeperEntity e) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/zookeeper.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ZooKeeperEntity e) {
        // Skin par METIER : chaque metier a son propre dossier de skins.
        // Depose tes skins (format Minecraft 64x64) dans :
        //   textures/entity/keeper/generalist/skin_1.png .. skin_8.png (polyvalent)
        //   textures/entity/keeper/vet/skin_1.png .. skin_8.png (veterinaire)
        //   textures/entity/keeper/feeder/skin_1.png .. skin_8.png (nourrisseur)
        //   textures/entity/keeper/guard/skin_1.png .. skin_8.png (garde)
        //   textures/entity/keeper/vendor/skin_1.png .. skin_8.png (vendeur)
        // Pour augmenter le nombre de skins par metier, augmente KEEPER_SKINS_PER_JOB
        // dans ZooKeeperEntity.
        String jobFolder = switch (e.getJob()) {
            case 1 -> "vet";
            case 2 -> "feeder";
            case 3 -> "guard";
            case 4 -> "vendor";
            case 5 -> "janitor";
            default -> "generalist";
        };
        int variant = e.getSkin() + 1;
        return new ResourceLocation(UltimateZooTame.MODID,
                "textures/entity/keeper/" + jobFolder + "/skin_" + variant + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ZooKeeperEntity e) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/zookeeper.animation.json");
    }
}
