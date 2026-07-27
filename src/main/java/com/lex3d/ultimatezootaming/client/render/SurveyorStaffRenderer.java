package com.lex3d.ultimatezootaming.client.render;

import com.lex3d.ultimatezootaming.items.SurveyorStaffItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SurveyorStaffRenderer extends GeoItemRenderer<SurveyorStaffItem> {
    public SurveyorStaffRenderer() {
        super(new SurveyorStaffModel());
    }
}
