package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.entity.GoblinEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Modelo para las manos del Goblin en GeckoLib 4.
 */
public class GoblinHandModel extends GeoModel<GoblinEntity> {

    @Override
    public ResourceLocation getModelResource(GoblinEntity animatable) {
        // Asegúrate de que este archivo .geo.json exista en tus assets
        return new ResourceLocation("sexmod", "geo/goblin/goblin_hand.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GoblinEntity animatable) {
        return new ResourceLocation("sexmod", "textures/entity/goblin/goblin.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GoblinEntity animatable) {
        return new ResourceLocation("sexmod", "animations/goblin/goblin_hand.animation.json");
    }
}