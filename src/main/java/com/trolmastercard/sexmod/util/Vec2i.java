package com.trolmastercard.sexmod.util;

/**
 * Vec2i - ported from e1.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Simple 2D integer coordinate pair. Provides a Euclidean distance helper
 * from this point to another (x, y) coordinate.
 *
 * No API changes required - this class uses only standard Java.
 */
public class Vec2i {

    public static final Vec2i ZERO = new Vec2i(0, 0);

    public int x;
    public int y;

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Euclidean distance from this point to {@code (toX, toY)}.
     */
    public float distanceTo(int toX, int toY) {
        float dx = (toX - this.x);
        float dy = (toY - this.y);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s)", x, y);
    }
}
