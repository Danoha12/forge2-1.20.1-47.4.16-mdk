package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.EllieEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * EllieHandModel — Portado a 1.20.1 / GeckoLib 4.
 */
public class EllieHandModel extends GeoModel<EllieEntity> {

    @Override
    public ResourceLocation getModelResource(EllieEntity animatable) {
        return new ResourceLocation("sexmod", "geo/ellie/ellie_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EllieEntity animatable) {
        return new ResourceLocation("sexmod", "textures/entity/ellie/ellie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EllieEntity animatable) {
        return new ResourceLocation("sexmod", "animations/ellie/ellie_hand.animation.json");
    }
}