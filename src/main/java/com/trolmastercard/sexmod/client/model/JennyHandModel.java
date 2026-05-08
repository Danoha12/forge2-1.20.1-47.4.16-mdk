package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.JennyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * JennyHandModel — Portado a 1.20.1 / GeckoLib 4.
 */
public class JennyHandModel extends GeoModel<JennyEntity> {

    @Override
    public ResourceLocation getModelResource(JennyEntity animatable) {
        return new ResourceLocation("sexmod", "geo/jenny/jenny_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JennyEntity animatable) {
        return new ResourceLocation("sexmod", "textures/entity/jenny/jenny.png");
    }

    @Override
    public ResourceLocation getAnimationResource(JennyEntity animatable) {
        return new ResourceLocation("sexmod", "animations/jenny/jenny_hand.animation.json");
    }
}