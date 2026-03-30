package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.BiaEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * BiaModel — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 *
 * GeoModel para la entidad NPC "Bia".
 * Maneja dos archivos de geometría: malla base y malla con vestimenta.
 * Incluye huesos para decoraciones (hojas) y capas de ropa superior e inferior.
 */
public class BiaModel extends BaseNpcModel<BiaEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/bia/bianude.geo.json"), // Malla base
                new ResourceLocation("sexmod", "geo/bia/biadressed.geo.json") // Malla vestida
        };
    }

    @Override
    public ResourceLocation getTextureResource(BiaEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/bia/bia.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BiaEntity entity) {
        return new ResourceLocation("sexmod", "animations/bia/bia.animation.json");
    }

    // =========================================================================
    //  Arreglos de huesos (Mapeo exacto a Blockbench)
    // =========================================================================

    @Override
    public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }

    /** Huesos decorativos (hojas en el cabello). */
    @Override
    public String[] getFeatureBones() {
        return new String[]{ "leaf7", "leaf8" };
    }

    @Override
    public String[] getChestBones() {
        return new String[]{ "armorChest", "armorBoobs", "armorShoulderR", "armorShoulderL" };
    }

    @Override
    public String[] getUpperFleshBones() {
        // "bra" es el nombre del hueso en Blockbench para la ropa superior
        return new String[]{ "bra", "upperBodyR", "upperBodyL" };
    }

    @Override
    public String[] getLowerArmorBones() {
        return new String[]{
                "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR",
                "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip"
        };
    }

    @Override
    public String[] getLowerFleshBones() {
        // "slip" es el nombre del hueso en Blockbench para la ropa inferior
        return new String[]{ "slip", "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    @Override
    public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}