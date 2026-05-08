package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * PlayerKoboldModel — Corregido para usar ModConstants.
 */
public class PlayerKoboldModel extends GeoModel<PlayerKoboldEntity> {

    @Override
    public ResourceLocation getModelResource(PlayerKoboldEntity animatable) {
        // Usamos ModConstants.MOD_ID en lugar de SexMod.MODID
        return new ResourceLocation(ModConstants.MOD_ID, "geo/entity/kobold.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(PlayerKoboldEntity animatable) {
        return new ResourceLocation(ModConstants.MOD_ID, "textures/entity/kobold/kobold.png");
    }

    @Override
    public ResourceLocation getAnimationResource(PlayerKoboldEntity animatable) {
        return new ResourceLocation(ModConstants.MOD_ID, "animations/entity/kobold.animation.json");
    }
}