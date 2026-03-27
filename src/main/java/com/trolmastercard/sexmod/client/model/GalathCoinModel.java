package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.item.GalathCoinItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GalathCoinModel - ported from as.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * {@link GeoModel} for the Galath Coin item ({@code cc} - {@link GalathCoinItem}).
 *
 * Resources:
 *   Geo  : sexmod:geo/galath/galath_coin.geo.json
 *   Tex  : sexmod:textures/items/galath_coin/galath_coin.png
 *   Anim : sexmod:animations/galath/galath_coin.animation.json
 *
 * In 1.12.2 this extended {@code AnimatedGeoModel<cc>} with methods ordered
 * b()/a()/c() for geo/texture/animation respectively.
 * In 1.20.1 we extend {@code GeoModel<T>} with clearly named overrides.
 */
public class GalathCoinModel extends GeoModel<GalathCoinItem> {

    @Override
    public ResourceLocation getModelResource(GalathCoinItem item) {
        return new ResourceLocation("sexmod", "geo/galath/galath_coin.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GalathCoinItem item) {
        return new ResourceLocation("sexmod", "textures/items/galath_coin/galath_coin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GalathCoinItem item) {
        return new ResourceLocation("sexmod", "animations/galath/galath_coin.animation.json");
    }
}
