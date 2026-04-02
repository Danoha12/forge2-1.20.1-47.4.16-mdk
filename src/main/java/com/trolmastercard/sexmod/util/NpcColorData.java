package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.util.NpcSkinTexture;
import net.minecraft.resources.ResourceLocation;
import java.util.HashSet;

/** NpcColorData - stub for NPC color/tinting data. */
public class NpcColorData {

    public static final NpcSkinTexture DEFAULT_TEXTURE = new NpcSkinTexture(
            new ResourceLocation("sexmod", "textures/entity/default.png")
    );

    /** Returns the set of bone names that should be hidden during rendering. */
    public static HashSet<String> getHiddenBones() {
        return new HashSet<>();
    }

    /** Applies outline tinting for the given entity (no-op stub). */
    public static void applyOutlineTinting(Object entity, float partialTick) {
        // stub - outline shader tinting
    }

    /** Applies tinting for the given entity (no-op stub). */
    public static void applyTinting(Object entity, float partialTick) {
        // stub
    }
}