package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.item.StaffItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * StaffModel — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * GeoModel para el ítem del bastón (StaffItem).
 * Define las rutas de los archivos de geometría, textura y animación.
 */
public class StaffModel extends GeoModel<StaffItem> {

    @Override
    public ResourceLocation getModelResource(StaffItem animatable) {
        return new ResourceLocation("sexmod", "geo/kobold/staff.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(StaffItem animatable) {
        return new ResourceLocation("sexmod", "textures/entity/kobold/staff.png");
    }

    @Override
    public ResourceLocation getAnimationResource(StaffItem animatable) {
        return new ResourceLocation("sexmod", "animations/kobold/staff.animation.json");
    }
}