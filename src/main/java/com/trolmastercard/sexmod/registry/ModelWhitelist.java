package com.trolmastercard.sexmod.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** ModelWhitelist - stub whitelist for custom NPC models. */
public class ModelWhitelist {
    public static ResourceLocation getGeoLocation(String name) {
        return new ResourceLocation("sexmod", "geo/" + name + ".geo.json");
    }
    public static ResourceLocation getTextureLocation(String name) {
        return new ResourceLocation("sexmod", "textures/entity/" + name + ".png");
    }
    public static boolean isWhitelisted(String name) { return true; }

    public static void reload(boolean b) {
    }

    public static List<String> getSerializedData() {
        return List.of();
    }
}