package com.trolmastercard.sexmod.client.model.item;

import com.trolmastercard.sexmod.item.GalathCoinItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GalathCoinModel — Portado a 1.20.1 / GeckoLib 4.
 * * Modelo Geo (3D) para el ítem de la Moneda de Galath.
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