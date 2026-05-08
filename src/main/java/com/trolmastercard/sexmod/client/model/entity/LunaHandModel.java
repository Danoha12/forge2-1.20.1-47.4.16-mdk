package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.entity.LunaEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * LunaHandModel — Portado a 1.20.1 / GeckoLib 4.
 * Define el modelo, textura y animaciones de las manos de Luna.
 */
public class LunaHandModel extends GeoModel<LunaEntity> {

    @Override
    public ResourceLocation getModelResource(LunaEntity animatable) {
        // Asegúrate de tener este .json en: assets/sexmod/geo/luna/luna_hand.geo.json
        return new ResourceLocation("sexmod", "geo/luna/luna_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(LunaEntity animatable) {
        // La textura suele ser la misma que la del cuerpo
        return new ResourceLocation("sexmod", "textures/entity/luna/luna.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LunaEntity animatable) {
        // Asegúrate de tener este .json en: assets/sexmod/animations/luna/luna_hand.animation.json
        return new ResourceLocation("sexmod", "animations/luna/luna_hand.animation.json");
    }
}