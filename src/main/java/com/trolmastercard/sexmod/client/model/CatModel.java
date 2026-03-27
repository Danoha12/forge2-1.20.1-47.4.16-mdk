package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.BaseNpcEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * CatModel - Portado a 1.20.1 / GeckoLib 4.
 * * Modelo para la entidad NPC "Cat".
 * Mantiene la jerarquía de huesos estándar para compatibilidad con armaduras
 * y añade soporte para huesos de tipo "cloth" para físicas de ropa.
 */
public class CatModel extends BaseNpcModel<BaseNpcEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                // Mantenemos las rutas para cargar tus archivos .json actuales
                new ResourceLocation("sexmod", "geo/cat/cat.geo.json"),
                new ResourceLocation("sexmod", "geo/cat/cat.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/cat/cat.png");
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "geo/cat/cat.geo.json");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/cat/cat.animation.json");
    }

    // =========================================================================
    //  Mapeado de Huesos (Strings intactos para evitar modelos invisibles)
    // =========================================================================

    /** Protección de cabeza */
    @Override public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }

    /** Armadura de Torso y Hombros */
    @Override public String[] getChestBones() {
        return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" };
    }

    /** Detalles del Torso (Incluye huesos de ropa/telas) */
    @Override public String[] getUpperFleshBones() {
        return new String[]{ "boobsFlesh", "cloth" };
    }

    /** Armadura de Piernas y Cintura */
    @Override public String[] getLowerArmorBones() {
        return new String[]{ "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR",
                "armorPantsLowR", "armorPantsUpR", "armorPantsUpL", "armorHip" };
    }

    /** Detalles de la Pelvis y Piernas (Incluye huesos de telas inferiores) */
    @Override public String[] getLowerFleshBones() {
        // Mantenemos "vagina" y "cloth" para que GeckoLib encuentre los huesos del modelo
        return new String[]{ "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR", "cloth" };
    }

    /** Calzado */
    @Override public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}