package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.item.StaffItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * StaffModel - ported from bc.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * {@link GeoModel} for the kobold staff item ({@code hy} - {@link StaffItem}).
 *
 * Resources:
 *   Geo  : sexmod:geo/kobold/staff.geo.json
 *   Tex  : sexmod:textures/entity/kobold/staff.png
 *   Anim : sexmod:animations/kobold/staff.animation.json
 *
 * In 1.12.2 the methods were ordered b()/a()/c() for geo/texture/animation.
 * In 1.20.1 we use the clearly named GeoModel overrides.
 */
public class StaffModel extends GeoModel<StaffItem> {

    @Override
    public ResourceLocation getModelResource(StaffItem item) {
        return new ResourceLocation("sexmod", "geo/kobold/staff.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StaffItem item) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/staff.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StaffItem item) {
        return new ResourceLocation("sexmod", "animations/kobold/staff.animation.json");
    }
}
