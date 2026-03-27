package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.ClothingOverlayEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * ClothingOverlayModel - ported from o.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 */
public class ClothingOverlayModel extends GeoModel<ClothingOverlayEntity> {

    private static final ResourceLocation CROSS_GEO =
            new ResourceLocation("sexmod", "geo/cross.geo.json");
    private static final ResourceLocation CROSS_TEX =
            new ResourceLocation("sexmod", "textures/cross.png");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation("sexmod", "animations/slime/slime.animation.json");

    @Override
    public ResourceLocation getModelResource(ClothingOverlayEntity entity) {
        if (entity.isPreviewMode()) return CROSS_GEO;
        String name = entity.getTextureName();
        return new ResourceLocation("sexmod", "geo/" + name + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ClothingOverlayEntity entity) {
        if (entity.isPreviewMode()) return CROSS_TEX;
        String name = entity.getTextureName();
        return new ResourceLocation("sexmod", "textures/entity/" + name + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ClothingOverlayEntity entity) {
        return ANIMATION;
    }
}