package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.JennyEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * JennyModel - ported from c5.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * GeoModel for JennyEntity.  Two geo files:
 *   [0] = sexmod:geo/jenny/jennynude.geo.json
 *   [1] = sexmod:geo/jenny/jennydressed.geo.json
 *
 * No custom model-selection logic - standard BaseNpcModel behaviour (MODEL_INDEX
 * selects the file index).
 *
 * Slot bone arrays mirror the kobold layout with slightly fewer lower bones.
 */
public class JennyModel extends BaseNpcModel<JennyEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
            new ResourceLocation("sexmod", "geo/jenny/jennynude.geo.json"),
            new ResourceLocation("sexmod", "geo/jenny/jennydressed.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(JennyEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/jenny/jenny.png");
    }

    @Override
    public ResourceLocation getModelResource(JennyEntity entity) {
        return new ResourceLocation("sexmod", "geo/jenny/jennynude.geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(JennyEntity entity) {
        return new ResourceLocation("sexmod", "animations/jenny/jenny.animation.json");
    }

    // ---- Slot bone arrays ---------------------------------------------------

    @Override public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }

    @Override public String[] getChestBones() {
        return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" };
    }

    @Override public String[] getUpperFleshBones() {
        return new String[]{ "boobsFlesh", "upperBodyL", "upperBodyR" };
    }

    @Override public String[] getLowerArmorBones() {
        return new String[]{
            "armorBootyR", "armorBootyL",
            "armorPantsLowL", "armorPantsLowR", "armorPantsLowR",
            "armorPantsUpR", "armorPantsUpL", "armorHip"
        };
    }

    @Override public String[] getLowerFleshBones() {
        return new String[]{ "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    @Override public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}
