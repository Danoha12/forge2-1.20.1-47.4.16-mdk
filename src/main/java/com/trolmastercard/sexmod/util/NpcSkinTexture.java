package com.trolmastercard.sexmod.util;

import net.minecraft.resources.ResourceLocation;

/** NpcSkinTexture - wraps a skin texture location for NPC rendering. */
public class NpcSkinTexture {
    public final ResourceLocation location;
    public final boolean isCustom;

    public NpcSkinTexture(ResourceLocation location, boolean isCustom) {
        this.location = location;
        this.isCustom = isCustom;
    }

    public NpcSkinTexture(ResourceLocation location) {
        this(location, false);
    }

    public ResourceLocation getLocation() { return location; }
    public boolean isCustom() { return isCustom; }
}