package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.JennyEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * JennyHandNudeModel — Versión sin ropa de las manos.
 */
public class JennyHandNudeModel extends GeoModel<JennyEntity> {

    @Override
    public ResourceLocation getModelResource(JennyEntity animatable) {
        // Si no tienes un .json específico para 'nude', usa el mismo de 'jenny_hand'
        return new ResourceLocation("sexmod", "geo/jenny/jenny_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(JennyEntity animatable) {
        // Aquí apunta a la textura de piel de Jenny (sin guantes/mangas)
        return new ResourceLocation("sexmod", "textures/entity/jenny/jenny_nude.png");
    }

    @Override
    public ResourceLocation getAnimationResource(JennyEntity animatable) {
        return new ResourceLocation("sexmod", "animations/jenny/jenny_hand.animation.json");
    }
}