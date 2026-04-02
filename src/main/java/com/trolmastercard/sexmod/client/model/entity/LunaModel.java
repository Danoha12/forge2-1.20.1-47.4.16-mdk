package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.client.model.BaseNpcModel;
import com.trolmastercard.sexmod.entity.LunaEntity;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.resources.ResourceLocation;

/**
 * LunaModel — Portado a 1.20.1 / GeckoLib 4.
 * * GeoModel para LunaEntity (previamente conocida como CatEntity en la 1.12.2).
 * * Define las rutas a los archivos JSON y mapea los huesos para la armadura y la ropa.
 */
public class LunaModel extends BaseNpcModel<LunaEntity> {

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation(ModConstants.MOD_ID, "geo/cat/cat.geo.json"),
                new ResourceLocation(ModConstants.MOD_ID, "geo/cat/cat.geo.json")
        };
    }

    @Override
    public ResourceLocation getTextureResource(LunaEntity entity) {
        return new ResourceLocation(ModConstants.MOD_ID, "textures/entity/cat/cat.png");
    }

    @Override
    public ResourceLocation getAnimationResource(LunaEntity entity) {
        return new ResourceLocation(ModConstants.MOD_ID, "animations/cat/cat.animation.json");
    }

    // ── Mapeo de Huesos (Armadura y Carne) ───────────────────────────────────

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
        return new String[]{ "boobsFlesh", "cloth" };
    }

    @Override
    public String[] getLowerArmorBones() {
        // Removido el duplicado de "armorPantsLowR"
        return new String[]{ "armorBootyR", "armorBootyL", "armorPantsLowL", "armorPantsLowR",
                "armorPantsUpR", "armorPantsUpL", "armorHip" };
    }

    @Override
    public String[] getLowerFleshBones() {
        return new String[]{ "fleshL", "fleshR", "vagina", "curvesL", "curvesR", "kneeL", "kneeR", "cloth" };
    }

    @Override
    public String[] getShoeBones() {
        return new String[]{ "armorShoesL", "armorShoesR" };
    }
}