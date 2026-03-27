package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.KoboldEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.Vec3;

/**
 * EyeAndKoboldColor - ported from EyeAndKoboldColor.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Dual-purpose color enum: stores kobold body color AND a matching wool meta ID
 * used as the item damage value for kobold eggs and tribe highlights.
 *
 * 1.12.2 - 1.20.1:
 *   - Vec3i - Vec3 (int coords as doubles)
 *   - TextFormatting stays as ChatFormatting (same class, renamed)
 *   - ff.aJ - KoboldEntity.DEFAULT_COLOR (referenced by safeValueOf fallback)
 */
public enum EyeAndKoboldColor {
    GREEN      ( 69,141,113,  91,167,128,  9, ChatFormatting.DARK_GREEN),
    YELLOW     (241,177, 77, 255,226,170,  4, ChatFormatting.YELLOW),
    RED        (230, 27, 57, 253,232,239, 14, ChatFormatting.RED),
    PURPLE     (196,148,207, 246,188, 96, 10, ChatFormatting.DARK_PURPLE),
    LIGHT_GREEN(170,208, 47, 230,214,104,  5, ChatFormatting.GREEN),
    OLD_BLUE   (173,138,128, 118,151,180,  2, ChatFormatting.LIGHT_PURPLE),
    DARK_GREY  ( 92, 92,110, 198,193,165,  7, ChatFormatting.DARK_GRAY),
    BROWN      (200,145,112, 253,228,198, 12, ChatFormatting.GOLD),
    DARK_BLUE  ( 65, 84,116, 104,137,146, 11, ChatFormatting.DARK_BLUE),
    LIGHT_BLUE (100,163,206, 138,235,242,  3, ChatFormatting.DARK_AQUA),
    SILVER     (136,136,134, 255,255,255,  0, ChatFormatting.GRAY);

    private final Vec3 mainColor;
    private final Vec3 secondaryColor;
    private final int  woolMeta;
    private final ChatFormatting textColor;

    EyeAndKoboldColor(int r1, int g1, int b1,
                       int r2, int g2, int b2,
                       int woolMeta, ChatFormatting textColor) {
        this.mainColor      = new Vec3(r1, g1, b1);
        this.secondaryColor = new Vec3(r2, g2, b2);
        this.woolMeta       = woolMeta;
        this.textColor      = textColor;
    }

    public Vec3 getMainColor()      { return mainColor; }
    public Vec3 getSecondaryColor() { return secondaryColor; }
    public int  getWoolMeta()       { return woolMeta; }
    public ChatFormatting getTextColor() { return textColor; }

    /** Returns the main color as an int[] {r, g, b}. */
    public int[] getMainColorRGB() {
        return new int[]{ (int)mainColor.x, (int)mainColor.y, (int)mainColor.z };
    }

    public static int indexOf(EyeAndKoboldColor color) {
        EyeAndKoboldColor[] vals = values();
        for (int i = 0; i < vals.length; i++)
            if (color == vals[i]) return i;
        return vals.length;
    }

    /** Returns the color by name, falling back to KoboldEntity.DEFAULT_COLOR. */
    public static EyeAndKoboldColor safeValueOf(String name) {
        try { return valueOf(name); }
        catch (IllegalArgumentException e) { return KoboldEntity.DEFAULT_COLOR; }
    }

    public static EyeAndKoboldColor safeValueOf(Vec3 vec) {
        for (EyeAndKoboldColor c : values())
            if (vec.equals(c.mainColor)) return c;
        return KoboldEntity.DEFAULT_COLOR;
    }

    public static EyeAndKoboldColor getColorByWoolId(int id) {
        for (EyeAndKoboldColor c : values())
            if (c.woolMeta == id) return c;
        return KoboldEntity.DEFAULT_COLOR;
    }

    /** Returns the enum value at index {@code i}, clamped. */
    public static EyeAndKoboldColor getByIndex(int i) {
        EyeAndKoboldColor[] vals = values();
        return vals[Math.max(0, Math.min(i, vals.length - 1))];
    }
}
