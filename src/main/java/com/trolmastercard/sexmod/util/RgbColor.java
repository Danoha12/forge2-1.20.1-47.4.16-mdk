package com.trolmastercard.sexmod.util;

/**
 * Simple RGB colour triple (float r, g, b).
 * Ported from f7.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 */
public class RgbColor {

    public static final RgbColor BLACK = new RgbColor(0.0F, 0.0F, 0.0F);

    /** Red channel [0..1] */
    public float r;
    /** Green channel [0..1] */
    public float g;
    /** Blue channel [0..1] */
    public float b;

    public RgbColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /** Returns {@code this - other} (component-wise). */
    public RgbColor subtract(RgbColor other) {
        return new RgbColor(this.r - other.r, this.g - other.g, this.b - other.b);
    }

    /** Returns {@code this + other} (component-wise). */
    public RgbColor add(RgbColor other) {
        return new RgbColor(this.r + other.r, this.g + other.g, this.b + other.b);
    }

    /** Returns {@code this * scale} (component-wise). */
    public RgbColor scale(float scale) {
        return new RgbColor(this.r * scale, this.g * scale, this.b * scale);
    }
}
