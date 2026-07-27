package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.blocks.KeeperLockerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KeeperLockerModel extends GeoModel<KeeperLockerBlockEntity> {
    @Override
    public ResourceLocation getModelResource(KeeperLockerBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/keeper_locker.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(KeeperLockerBlockEntity be) {
        String variant = "wood";
        if (be.getBlockState().getBlock() instanceof com.lex3d.ultimatezootaming.blocks.KeeperLockerBlock b) {
            variant = b.getVariant();
        }
        String tex = variant.equals("wood")
                ? "textures/block/keeper_locker_3d.png"
                : "textures/block/keeper_locker_" + variant + ".png";
        return new ResourceLocation(UltimateZooTame.MODID, tex);
    }
    @Override
    public ResourceLocation getAnimationResource(KeeperLockerBlockEntity be) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/keeper_locker.animation.json");
    }
}
