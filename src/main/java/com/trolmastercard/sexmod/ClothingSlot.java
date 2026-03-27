package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.ModConstants;

/**
 * ClothingSlot - clothing overlay slot identifiers for the NPC customise screen.
 * Ported from gw.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 */
public enum ClothingSlot {

    GIRL_SPECIFIC,
    HEAD        (0,   "customHead"),
    FOOT_L      (60,  "customShoeL"),
    FOOT_R      (80,  "customShoeR"),
    HAND_L      (100, "customHandL"),
    HAND_R      (120, "customHandR"),
    CUSTOM_BONE (140);

    public static final String SEPARATOR = "#";

    public int buttonIDPlus;
    public int buttonIDMinus;
    public String boneName = null;
    public int iconXPos = 0;

    ClothingSlot(int iconXPos) {
        this.iconXPos = iconXPos;
    }

    ClothingSlot(int iconXPos, String boneName) {
        this.iconXPos  = iconXPos;
        this.boneName  = boneName;
        this.buttonIDPlus  = ++ModConstants.BUTTON_ID_COUNTER;
        this.buttonIDMinus = ++ModConstants.BUTTON_ID_COUNTER;
    }

    /** Number of bone-mapped slots (excludes GIRL_SPECIFIC and CUSTOM_BONE). */
    public static int boneMappedCount() {
        return values().length - 2;
    }
}
