package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * AllieModel — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 *
 * GeoModel para AllieEntity. Contiene tres entradas de recursos geo:
 * [0] = sexmod:geo/allie/allie.geo.json   (Malla base)
 * [1] = sexmod:geo/allie/armored.geo.json (Capa de armadura)
 * [2] = sexmod:geo/allie/allie.geo.json   (Repetido para el índice de vestimenta 1)
 */
public class AllieModel extends BaseNpcModel<AllieEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/allie/allie.geo.json"),
                new ResourceLocation("sexmod", "geo/allie/armored.geo.json"),
                new ResourceLocation("sexmod", "geo/allie/allie.geo.json")
        };
    }

    @Override
    public ResourceLocation getModelResource(AllieEntity entity) {
        ResourceLocation[] files = getGeoFiles();

        // En mundos falsos/menús de renderizado UI -> siempre usar la malla base
        if (entity.level() == null) return files[0];

        int modelIdx = entity.getEntityData().get(BaseNpcEntity.MODEL_INDEX);

        // Protección contra índices fuera de rango
        if (modelIdx >= files.length || modelIdx < 0) {
            System.out.println("[AllieModel] Allie doesn't have outfit Nr." + modelIdx + ", defaulting to base model.");
            return files[0];
        }

        // Si es una sub-variante (usa su propio índice directamente)
        if (entity.isSubVariant()) {
            return files[modelIdx];
        }

        // Índice 1 -> Variante vestida (índice 2 en el arreglo)
        if (modelIdx == 1) return files[2];

        // Por defecto -> Malla base (0)
        return files[0];
    }

    @Override
    public ResourceLocation getTextureResource(AllieEntity entity) {
        return new ResourceLocation("sexmod", "textures/entity/allie/allie.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AllieEntity entity) {
        return new ResourceLocation("sexmod", "animations/allie/allie.animation.json");
    }

    // ---- Slot bone arrays (Mapeados directos de Blockbench) -----------------

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
        return new String[]{ "boobsFlesh", "clothes", "clothesR", "clothesL" };
    }
}