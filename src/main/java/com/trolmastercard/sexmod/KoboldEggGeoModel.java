package com.trolmastercard.sexmod;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldEggGeoModel - ported from k.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * GeckoLib4 GeoModel for the KoboldEggEntity.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - AnimatedGeoModel&lt;i&gt; - GeoModel&lt;KoboldEggEntity&gt;
 *   - b(i) - getModelResource(KoboldEggEntity)
 *   - c(i) - getTextureResource(KoboldEggEntity)
 *   - a(i) - getAnimationResource(KoboldEggEntity)
 */
public class KoboldEggGeoModel extends GeoModel<KoboldEggEntity> {

    @Override
    public ResourceLocation getModelResource(KoboldEggEntity egg) {
        return new ResourceLocation("sexmod", "geo/kobold/koboldegg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KoboldEggEntity egg) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/koboldegg.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldEggEntity egg) {
        return new ResourceLocation("sexmod", "animations/kobold/egg.animation.json");
    }
}
