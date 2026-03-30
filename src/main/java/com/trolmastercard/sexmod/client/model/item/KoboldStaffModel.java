package com.trolmastercard.sexmod.client.model.item;

import com.trolmastercard.sexmod.item.KoboldStaffItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldStaffModel — Portado a 1.20.1 / GeckoLib 4.
 * * Vincula el modelo 3D, la textura y las animaciones al ítem del báculo.
 */
public class KoboldStaffModel extends GeoModel<KoboldStaffItem> {

    @Override
    public ResourceLocation getModelResource(KoboldStaffItem animatable) {
        return new ResourceLocation("sexmod", "geo/kobold/staff.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KoboldStaffItem animatable) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/staff.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KoboldStaffItem animatable) {
        return new ResourceLocation("sexmod", "animations/kobold/staff.animation.json");
    }
}