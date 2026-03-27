package com.trolmastercard.sexmod.util;

/**
 * RgbaColorInner - lightweight RGBA colour container (r, g, b, a as 0-255 ints).
 * Ported from gv.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Note: The original field order was a=R, d=G, c=B, b=A.
 */
public class RgbaColorInner {

    public int r;
    public int g;
    public int b;
    public int a;

    public RgbaColorInner(int r, int g, int b, int a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
}
