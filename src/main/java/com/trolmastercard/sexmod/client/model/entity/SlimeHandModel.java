package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.entity.SlimeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * SlimeHandModel — Portado a 1.20.1 / GeckoLib 4.
 * Define el modelo, textura y animaciones de las manos del Slime.
 */
public class SlimeHandModel extends GeoModel<SlimeEntity> {

    @Override
    public ResourceLocation getModelResource(SlimeEntity animatable) {
        // Asegúrate de tener este .json en: assets/sexmod/geo/slime/slime_hand.geo.json
        return new ResourceLocation("sexmod", "geo/slime/slime_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SlimeEntity animatable) {
        // Usamos la textura base del Slime
        return new ResourceLocation("sexmod", "textures/entity/slime/slime.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SlimeEntity animatable) {
        // Asegúrate de tener este .json en: assets/sexmod/animations/slime/slime_hand.animation.json
        return new ResourceLocation("sexmod", "animations/slime/slime_hand.animation.json");
    }
}