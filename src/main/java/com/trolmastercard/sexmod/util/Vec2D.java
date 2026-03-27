package com.trolmastercard.sexmod.util;

/**
 * Simple two-component double vector used for chunk coordinate comparisons.
 * Obfuscated name: g8
 */
public class Vec2D {
    public double x;
    public double z;

    public Vec2D(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public Vec2D subtract(Vec2D other) {
        return new Vec2D(this.x - other.x, this.z - other.z);
    }
}
