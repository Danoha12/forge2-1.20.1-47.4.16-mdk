package com.trolmastercard.sexmod.client.model.item;

import com.trolmastercard.sexmod.item.KoboldEggSpawnItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldEggItemModel — Portado a 1.20.1 / GeckoLib 4.
 * * Define los recursos visuales para el ítem del huevo en 3D.
 * - Utiliza la misma geometría y animaciones que la entidad para ahorrar recursos.
 */
public class KoboldEggItemModel extends GeoModel<KoboldEggSpawnItem> {

    @Override
    public ResourceLocation getModelResource(KoboldEggSpawnItem animatable) {
        // Reutilizamos el modelo de la entidad
        return new ResourceLocation("sexmod", "geo/kobold/koboldegg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KoboldEggSpawnItem animatable) {
        // Reutilizamos la textura de la entidad
        return new ResourceLocation("sexmod", "textures/entity/kobold/koboldegg.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldEggSpawnItem animatable) {
        // Reutilizamos las animaciones de balanceo (rocking)
        return new ResourceLocation("sexmod", "animations/kobold/egg.animation.json");
    }
}