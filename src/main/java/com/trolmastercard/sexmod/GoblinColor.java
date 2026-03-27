package com.trolmastercard.sexmod;

import net.minecraft.core.Vec3i;

/**
 * Color variants for Goblin NPCs, stored as RGB Vec3i values.
 * Obfuscated name: g5
 */
public enum GoblinColor {
    PURPLE(103,  39, 123),
    ORANGE(251, 153,  56),
    BLACK (  30, 33,  38),
    BLUE  ( 88,  83, 186),
    BROWN ( 63,  35,  34),
    PINK  (247, 102, 109),
    RED   (241,  69,  49),
    GREEN ( 75, 143, 106);

    private final Vec3i rgb;

    GoblinColor(int r, int g, int b) {
        this.rgb = new Vec3i(r, g, b);
    }

    public Vec3i getRgb() {
        return this.rgb;
    }

    /** Returns the ordinal index of the given color. */
    public static int indexOf(GoblinColor color) {
        int i = 0;
        for (GoblinColor c : values()) {
            if (color == c) return i;
            i++;
        }
        return i;
    }
}
