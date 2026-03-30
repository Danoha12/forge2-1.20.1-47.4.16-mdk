package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.JennyEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * JennyModel — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja los archivos de geometría, texturas y animaciones para Jenny.
 * Soporta múltiples archivos .geo para el sistema de cambio de vestimenta (Dressed/Minimal).
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

    /**
     * Selecciona dinámicamente el archivo de geometría basado en el estado del NPC.
     */
    @Override
    public ResourceLocation getModelResource(JennyEntity entity) {
        ResourceLocation[] files = getGeoFiles();
        int index = entity.getEntityData().get(BaseNpcEntity.MODEL_INDEX);

        // Validación de seguridad para evitar OutOfBounds
        if (index >= 0 && index < files.length) {
            return files[index];
        }
        return files[0];
    }

    @Override
    public ResourceLocation getAnimationResource(JennyEntity entity) {
        return new ResourceLocation("sexmod", "animations/jenny/jenny.animation.json");
    }

    // ── Arreglos de Huesos para Armaduras y Flesh ────────────────────────────

    @Override
    public String[] getHelmetBones() {
        return new String[]{ "armorHelmet" };
    }

    @Override
    public String[] getChestBones() {
        return new String[]{ "armorShoulderR", "armorShoulderL", "armorChest", "armorBoobs" };
    }

    @Override
    public String[] getUpperFleshBones() {
        return new String[]{ "boobsFlesh", "upperBodyL", "upperBodyR" };
    }

    @Override
    public String[] getLowerArmorBones() {
        return new String[]{
                "armorBootyR", "armorBootyL",
                "armorPantsLowL", "armorPantsLowR",
                "armorPantsUpR", "armorPantsUpL", "armorHip"
        };
    }

    @Override
    public String[] getLowerFleshBones() {
        // Huesos que se ocultan cuando Jenny tiene armadura puesta
        return new String[]{ "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR" };
    }

    @Override
    public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}