package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.item.KoboldEggSpawnItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * GeckoLib4 model for the {@link KoboldEggSpawnItem}.
 * Ported from f6.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 */
public class KoboldEggItemModel extends GeoModel<KoboldEggSpawnItem> {

    @Override
    public ResourceLocation getModelResource(KoboldEggSpawnItem animatable) {
        return new ResourceLocation("sexmod", "geo/kobold/koboldegg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KoboldEggSpawnItem animatable) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/koboldegg.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldEggSpawnItem animatable) {
        return new ResourceLocation("sexmod", "animations/kobold/egg.animation.json");
    }
}
