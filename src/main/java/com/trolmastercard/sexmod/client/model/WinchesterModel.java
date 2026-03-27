package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.item.WinchesterItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * WinchesterModel - ported from a2.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * {@link GeoModel} for the Winchester item (aj - {@link WinchesterItem}).
 *
 * Resources:
 *   Geo  : sexmod:geo/west/winchester.geo.json
 *   Tex  : sexmod:textures/items/winchester/winchester.png
 *   Anim : sexmod:animations/west/winchester.animation.json
 *
 * In GeckoLib 3 the model extended {@code AnimatedGeoModel<aj>} and
 * parameter order was c()/b()/a() for geo/texture/animation.
 * In GeckoLib 4 the class extends {@code GeoModel<T>} and the three methods
 * are {@code getModelResource}, {@code getTextureResource}, and
 * {@code getAnimationResource}.
 */
public class WinchesterModel extends GeoModel<WinchesterItem> {

    @Override
    public ResourceLocation getModelResource(WinchesterItem item) {
        return new ResourceLocation("sexmod", "geo/west/winchester.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(WinchesterItem item) {
        return new ResourceLocation("sexmod", "textures/items/winchester/winchester.png");
    }

    @Override
    public ResourceLocation getAnimationResource(WinchesterItem item) {
        return new ResourceLocation("sexmod", "animations/west/winchester.animation.json");
    }
}
