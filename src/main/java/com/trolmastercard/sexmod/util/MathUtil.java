package com.trolmastercard.sexmod.util;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/**
 * MathUtil - ported from b6.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Static math utilities: interpolation, easing functions, Vec3 / Vec3i helpers.
 *
 * Method mapping (obfuscated - clean), verified against decompiled source:
 *
 *   a(Vec3d,Vec3d,int)          - stepTowards(Vec3,Vec3,int)
 *   b(double,double,double)     - lerp(double,double,double)
 *   a(float,float,float)        - lerp(float,float,float)
 *   a(float,float,double)       - lerpAngle(float,float,double)   - radian wrap
 *   b(float,float,double)       - lerpAngleDeg(float,float,double)
 *   a(Vec3d,Vec3d,double)       - lerpPosition(Vec3,Vec3,double)
 *   a(f7,f7,double)             - lerpColor(RgbColor,RgbColor,double)
 *   a(Vec3i,Vec3i,double)       - lerpVec3i(Vec3i,Vec3i,double)
 *   a(gv,gv,double)             - lerpRgba(int[],int[],double)  - [R,G,B,A]
 *   e(double)                   - easeOutQuart(double)
 *   g(double)                   - easeOutCubic(double)
 *   c(double)                   - easeOutBack(double)
 *   d(double)                   - easeInBack(double)
 *   b(double)                   - easeInSine(double)
 *   a(double)                   - easeCubic(double)              - t-
 *   h(double)                   - easeInOutSine(double)
 *   f(double)                   - easeInCosine(double)
 *   a(double,double,double)     - cosineInterp(double,double,double)
 */
public final class MathUtil {

    private MathUtil() {}

    // =========================================================================
    //  a(Vec3d, Vec3d, int) - stepTowards
    // =========================================================================

    /** Moves {@code from} one step toward {@code to} out of {@code steps}. */
    public static Vec3 stepTowards(Vec3 from, Vec3 to, int steps) {
        if (steps == 0) return to;
        Vec3 delta = to.subtract(from);
        return from.add(delta.x / steps, delta.y / steps, delta.z / steps);
    }

    // =========================================================================
    //  Lerp overloads
    // =========================================================================

    /** b(double,double,double) - double lerp */
    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** a(float,float,float) - float lerp */
    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /**
     * a(float,float,double) - radian-aware angle lerp.
     * Wraps the difference into [--, -] before interpolating.
     */
    public static float lerpAngle(float from, float to, double t) {
        float delta = to - from;
        while (delta < -Math.PI) delta += (float)(2 * Math.PI);
        while (delta >= Math.PI)  delta -= (float)(2 * Math.PI);
        return (float)(from + delta * t);
    }

    /** b(float,float,double) - degree-angle lerp (converts to radians first). */
    public static float lerpAngleDeg(float from, float to, double t) {
        return (float)Math.toDegrees(
            lerpAngle((float)Math.toRadians(from), (float)Math.toRadians(to), t));
    }

    /** a(Vec3d,Vec3d,double) - Vec3 lerp. */
    public static Vec3 lerpPosition(Vec3 from, Vec3 to, double t) {
        Vec3 delta = to.subtract(from);
        return from.add(delta.x * t, delta.y * t, delta.z * t);
    }

    /**
     * a(f7,f7,double) - RgbColor lerp.
     * f7 = RgbColor (fields: a=r, b=g, c=b)
     */
    public static RgbColor lerpColor(RgbColor from, RgbColor to, double t) {
        return from.add(to.subtract(from).scale((float)t));
    }

    /** a(Vec3i,Vec3i,double) - Vec3i lerp. */
    public static Vec3i lerpVec3i(Vec3i from, Vec3i to, double t) {
        Vec3 delta = new Vec3(
            to.getX() - from.getX(),
            to.getY() - from.getY(),
            to.getZ() - from.getZ());
        return new Vec3i(
            (int)(from.getX() + delta.x * t),
            (int)(from.getY() + delta.y * t),
            (int)(from.getZ() + delta.z * t));
    }

    /**
     * a(gv,gv,double) - RGBA int-array lerp.
     * gv fields: a=R, d=G, c=B, b=A - array order [R,G,B,A]
     */
    public static int[] lerpRgba(int[] from, int[] to, double t) {
        return new int[]{
            (int)(from[0] + (to[0] - from[0]) * t),
            (int)(from[1] + (to[1] - from[1]) * t),
            (int)(from[2] + (to[2] - from[2]) * t),
            (int)(from[3] + (to[3] - from[3]) * t)
        };
    }

    // =========================================================================
    //  Easing functions
    // =========================================================================

    /** e(double) - 1-(1-t)^4 */
    public static double easeOutQuart(double t) {
        return 1.0 - Math.pow(1.0 - t, 4.0);
    }

    /** g(double) - 1-(1-t)^3 */
    public static double easeOutCubic(double t) {
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    /** c(double) - easeOutBack */
    public static double easeOutBack(double t) {
        final double c1 = 1.70158, c2 = c1 + 1.0;
        return 1.0 + c2 * Math.pow(t - 1.0, 3.0) + c1 * Math.pow(t - 1.0, 2.0);
    }

    /** d(double) - easeInBack */
    public static double easeInBack(double t) {
        final double c1 = 1.70158, c2 = c1 + 1.0;
        return c2 * t * t * t - c1 * t * t;
    }

    /** b(double) - sin(t * -/2) */
    public static double easeInSine(double t) {
        return Math.sin(t * Math.PI / 2.0);
    }

    /** a(double) - t- */
    public static double easeCubic(double t) {
        return t * t * t;
    }

    /** h(double) - -(cos(-t)-1)/2 */
    public static double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1.0) / 2.0;
    }

    /** f(double) - 1-cos(-t/2) */
    public static double easeInCosine(double t) {
        return 1.0 - Math.cos(Math.PI * t / 2.0);
    }

    /** a(double,double,double) - cosine interpolation */
    public static double cosineInterp(double a, double b, double t) {
        double mu = (1.0 - Math.cos(t * Math.PI)) / 2.0;
        return a * (1.0 - mu) + b * mu;
    }
}
