package com.trolmastercard.sexmod.util;

import java.util.Set;

/** ClothingOverlayBones - bone names used by the clothing overlay renderer. */
public class ClothingOverlayBones {

    /** All bone names that belong to the clothing overlay layer. */
    public static final Set<String> ALL = Set.of(
            "bra", "lower", "shoes", "helmet", "chestplate", "leggings", "boots",
            "clothing_bra", "clothing_lower", "clothing_shoes"
    );

    private ClothingOverlayBones() {}
}