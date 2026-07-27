package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.UltimateZooTame;
import com.lex3d.ultimatezootaming.items.SurveyorStaffItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SurveyorStaffModel extends GeoModel<SurveyorStaffItem> {
    @Override
    public ResourceLocation getModelResource(SurveyorStaffItem item) {
        return new ResourceLocation(UltimateZooTame.MODID, "geo/surveyor_staff.geo.json");
    }
    @Override
    public ResourceLocation getTextureResource(SurveyorStaffItem item) {
        return new ResourceLocation(UltimateZooTame.MODID, "textures/item/surveyor_staff_3d.png");
    }
    @Override
    public ResourceLocation getAnimationResource(SurveyorStaffItem item) {
        return new ResourceLocation(UltimateZooTame.MODID, "animations/surveyor_staff.animation.json");
    }
}
