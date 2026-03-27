package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.item.KoboldStaffItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldStaffModel - ported from bc.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * {@link GeoModel} for the Kobold Staff item ({@code hy} - {@link KoboldStaffItem}).
 *
 * Resources:
 *   Geo  : sexmod:geo/kobold/staff.geo.json
 *   Tex  : sexmod:textures/entity/kobold/staff.png
 *   Anim : sexmod:animations/kobold/staff.animation.json
 *
 * In 1.12.2: {@code AnimatedGeoModel<hy>} with {@code b()/a()/c()} method order.
 * In 1.20.1: {@code GeoModel<T>} with named overrides.
 */
public class KoboldStaffModel extends GeoModel<KoboldStaffItem> {

    @Override
    public ResourceLocation getModelResource(KoboldStaffItem item) {
        return new ResourceLocation("sexmod", "geo/kobold/staff.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KoboldStaffItem item) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/staff.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldStaffItem item) {
        return new ResourceLocation("sexmod", "animations/kobold/staff.animation.json");
    }
}
