package com.trolmastercard.sexmod.util;

/**
 * RgbaColor - ported from bl.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Plain mutable RGBA colour container (int fields).
 *
 * Field mapping:
 *   a = R (red)
 *   d = G (green)
 *   c = B (blue)
 *   b = A (alpha)
 *
 * Note: this is the mutable int-field version.
 * The inner record NpcBoneQuadBuilder.RgbaColor (gv) is a separate
 * immutable record used only inside NpcBoneQuadBuilder.
 */
public final class RgbaColor {

    /** Red channel.   Original field: a */
    public int r;
    /** Alpha channel. Original field: b */
    public int a;
    /** Blue channel.  Original field: c */
    public int b_field;
    /** Green channel. Original field: d */
    public int g;

    public RgbaColor(int r, int g, int b, int a) {
        this.r       = r;
        this.g       = g;
        this.b_field = b;
        this.a       = a;
    }

    public int pack() {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b_field & 0xFF);
    }
}
