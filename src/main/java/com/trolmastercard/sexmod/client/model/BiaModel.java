package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.BaseNpcEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * BiaModel - Portado a 1.20.1 / GeckoLib 4.
 * * Modelo para la entidad Bia.
 * Incluye soporte para modelos base y con vestimenta, además de
 * decoraciones especiales de hojas en el cabello.
 */
public class BiaModel extends BaseNpcModel<BaseNpcEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                // Mantenemos las rutas originales para que cargue tus archivos actuales
                new ResourceLocation("sexmod", "geo/bia/bianude.geo.json"),
                new ResourceLocation("sexmod", "geo/bia/biadressed.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/bia/bia.png");
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        // Por defecto cargamos el modelo base
        return new ResourceLocation("sexmod", "geo/bia/bianude.geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/bia/bia.animation.json");
    }

    // =========================================================================
    //  Mapeado de Huesos (Strings intactos para evitar errores de renderizado)
    // =========================================================================

    /** Casco y protección de cabeza */
    @Override public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }

    /** Decoraciones especiales (Hojas en el cabello) */
    @Override public String[] getFeatureBones() {
        return new String[]{ "leaf7", "leaf8" };
    }

    /** Armadura de Pecho y Hombros */
    @Override public String[] getChestBones() {
        return new String[]{ "armorChest", "armorBoobs", "armorShoulderR", "armorShoulderL" };
    }

    /** Detalles del Torso (Incluye el top/bra original) */
    @Override public String[] getUpperFleshBones() {
        return new String[]{ "bra", "upperBodyR", "upperBodyL" };
    }

    /** Armadura de Piernas y Cadera */
    @Override public String[] getLowerArmorBones() {
        return new String[]{ "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR",
                "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip" };
    }

    /** Detalles de la Pelvis y Piernas (Mantenemos los IDs para el .geo) */
    @Override public String[] getLowerFleshBones() {
        return new String[]{ "slip", "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    /** Calzado */
    @Override public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}