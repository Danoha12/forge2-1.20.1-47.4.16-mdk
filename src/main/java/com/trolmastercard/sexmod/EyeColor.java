package com.trolmastercard.sexmod;

import net.minecraft.world.phys.Vec3;

/**
 * EyeColor - ported from eh.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Maps eye color enum values to RGB Vec3 vectors.
 *
 * 1.12.2 - 1.20.1: Vec3i - Vec3 (using int coords cast to double).
 */
public enum EyeColor {
    RED(255, 0, 0),
    VIOLET(132, 30, 156),
    YELLOW(243, 247, 0),
    BROWN(105, 60, 9),
    TURKEY(0, 206, 217),
    BLUE(0, 0, 255);

    private final Vec3 colorVec;

    EyeColor(int r, int g, int b) {
        this.colorVec = new Vec3(r, g, b);
    }

    public Vec3 getColorVec() { return colorVec; }

    public int[] getRGB() {
        return new int[]{ (int)colorVec.x, (int)colorVec.y, (int)colorVec.z };
    }

    public static EyeColor fromVec(Vec3 v) {
        for (EyeColor c : values())
            if (v.equals(c.colorVec)) return c;
        return RED;
    }

    public static int indexOf(EyeColor color) {
        EyeColor[] vals = values();
        for (int i = 0; i < vals.length; i++)
            if (color == vals[i]) return i;
        return 0;
    }
}
