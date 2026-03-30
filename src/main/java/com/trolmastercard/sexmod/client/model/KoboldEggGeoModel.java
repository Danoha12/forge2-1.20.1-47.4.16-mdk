package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.KoboldEggEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldEggGeoModel — Portado a 1.20.1 / GeckoLib 4.
 * * Vincula los archivos de recursos (Geo, Texture, Animation) a la entidad del huevo.
 */
public class KoboldEggGeoModel extends GeoModel<KoboldEggEntity> {

    @Override
    public ResourceLocation getModelResource(KoboldEggEntity animatable) {
        return new ResourceLocation("sexmod", "geo/kobold/koboldegg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KoboldEggEntity animatable) {
        // Podrías añadir lógica aquí para cambiar la textura según el color del huevo
        // usando animatable.getEntityData().get(KoboldEggEntity.EGG_COLOR)
        return new ResourceLocation("sexmod", "textures/entity/kobold/koboldegg.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldEggEntity animatable) {
        return new ResourceLocation("sexmod", "animations/kobold/egg.animation.json");
    }
}