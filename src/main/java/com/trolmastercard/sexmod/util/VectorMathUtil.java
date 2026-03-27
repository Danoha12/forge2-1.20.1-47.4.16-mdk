package com.trolmastercard.sexmod.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * VectorMathUtil - ported from ck.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Pure-math static helpers for Vec3 operations used throughout the mod's
 * sex-animation positioning and rendering system.
 *
 * All trigonometry angles are in DEGREES unless noted otherwise.
 *
 * Method mapping (original overloaded "a"/"b"/"c" - descriptive names):
 *
 *   ck.a(Vec3d, double)                   - scale(Vec3, double)
 *   ck.a(Vector3f, Vec3d)                 - dot(Vector3f, Vec3)
 *   ck.a(Vec3d, Vec3d)         [double]   - dot(Vec3, Vec3)
 *   ck.b(Vec3d, Vec3d)                    - cross(Vec3, Vec3)
 *   ck.a(double,double,double, float)     - rotateYaw(double,double,double,float)
 *   ck.a(Vec3d, float)                    - rotateYaw(Vec3, float)         [xRot=0]
 *   ck.a(Vec3d, float, float)             - rotateYaw(Vec3, float, float)  [xRot, yaw]
 *   ck.a(double,double,double,float,float)- rotateYaw(double-3, float, float)
 *   ck.a(Vec3d, float, float, float)      - rotateEuler(Vec3, xDeg, yDeg, zDeg)
 *   ck.c(Vec3d)                           - flipXZ(Vec3)
 *   ck.a(Vec3d)                [negate XY]- negateXY(Vec3)
 *   ck.b(Vec3d)                [negate YZ]- negateYZ(Vec3)
 *   ck.a(double,double,double) [lerp t]   - inverseLerp(double,double,double)
 *   ck.a(Vec3d,Vec3d,Vec3d)   [lerp t]   - inverseLerpX(Vec3,Vec3,Vec3)
 */
public final class VectorMathUtil {

    private static final double TO_RAD = 0.017453292519943295;

    private VectorMathUtil() {}

    // =========================================================================
    //  Scale / dot / cross
    // =========================================================================

    /** Multiplies all components of {@code v} by {@code scalar}. */
    public static Vec3 scale(Vec3 v, double scalar) {
        return new Vec3(v.x * scalar, v.y * scalar, v.z * scalar);
    }

    /** Dot product of a JOML {@link Vector3f} and a Minecraft {@link Vec3}. */
    public static double dot(Vector3f a, Vec3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    /** Dot product of two {@link Vec3} vectors. */
    public static double dot(Vec3 a, Vec3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    /** Cross product {@code a - b}. */
    public static Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(
            a.y * b.z - a.z * b.y,
            a.z * b.x - a.x * b.z,
            a.x * b.y - a.y * b.x);
    }

    // =========================================================================
    //  Yaw-based rotation (entity horizontal rotation)
    // =========================================================================

    /**
     * Rotates a vector by X-roll {@code xRotDeg} and then by yaw {@code yawDeg},
     * where yaw is the standard Minecraft entity yaw (positive = clockwise
     * when viewed from above).
     *
     * Step 1 - X-axis roll (rotates Y/Z plane):
     * <pre>
     *   y' = y-cos(xRot) - z-sin(xRot)
     *   z' = y-sin(xRot) + z-cos(xRot)
     * </pre>
     * Step 2 - Yaw (rotates X/Z plane around Y):
     * <pre>
     *   x' = -sin(yaw+90-)-x' - sin(yaw)-z'
     *   z' =  cos(yaw+90-)-x' + cos(yaw)-z'
     * </pre>
     *
     * Equivalent to: {@code ck.a(Vec3d, float xRot, float yaw)}
     */
    public static Vec3 rotateYaw(Vec3 v, float xRotDeg, float yawDeg) {
        double xRad = xRotDeg * TO_RAD;
        double yRad = yawDeg  * TO_RAD;

        // Step 1 - X roll
        double newY = v.y * Math.cos(xRad) - v.z * Math.sin(xRad);
        double newZ = v.y * Math.sin(xRad) + v.z * Math.cos(xRad);
        double newX = v.x;

        // Step 2 - Yaw
        double outX = -Math.sin((yawDeg + 90.0F) * TO_RAD) * newX - Math.sin(yRad) * newZ;
        double outZ =  Math.cos((yawDeg + 90.0F) * TO_RAD) * newX + Math.cos(yRad) * newZ;

        return new Vec3(outX, newY, outZ);
    }

    /**
     * Rotates {@code v} by {@code yawDeg} only (X-roll = 0).
     *
     * Equivalent to: {@code ck.a(Vec3d, float yaw)}
     */
    public static Vec3 rotateYaw(Vec3 v, float yawDeg) {
        return rotateYaw(v, 0.0F, yawDeg);
    }

    /**
     * Convenience overload that constructs the Vec3 from components first.
     *
     * Equivalent to: {@code ck.a(double, double, double, float)}
     */
    public static Vec3 rotateYaw(double x, double y, double z, float yawDeg) {
        return rotateYaw(new Vec3(x, y, z), yawDeg);
    }

    /**
     * Convenience overload - components + both rotation angles.
     *
     * Equivalent to: {@code ck.a(double, double, double, float, float)}
     */
    public static Vec3 rotateYaw(double x, double y, double z,
                                  float xRotDeg, float yawDeg) {
        return rotateYaw(new Vec3(x, y, z), xRotDeg, yawDeg);
    }

    // =========================================================================
    //  Full Euler rotation  (X - Y - Z intrinsic)
    // =========================================================================

    /**
     * Applies intrinsic Euler rotations in X-Y-Z order.
     *
     * All three angles are first converted from GeckoLib bone-radians to
     * degrees via {@link ItemRenderUtil#boneRotToDegrees} (the original
     * called {@code gc.c(float)}), then trigonometric functions are applied.
     *
     * Rotation sequence:
     * <ol>
     *   <li>X-axis: {@code y' = y-cos(x) - z-sin(x)},  {@code z' = y-sin(x) + z-cos(x)}</li>
     *   <li>Y-axis: {@code x' = x-cos(y) + z-sin(y)},  {@code z' = -x-sin(y) + z-cos(y)}</li>
     *   <li>Z-axis: {@code x' = x-cos(z) - y-sin(z)},  {@code y' = x-sin(z) + y-cos(z)}</li>
     * </ol>
     *
     * Equivalent to: {@code ck.a(Vec3d, float, float, float)}
     *
     * @param v       the vector to rotate
     * @param xRad    X bone rotation in GeckoLib radians (will be converted to degrees)
     * @param yRad    Y bone rotation in GeckoLib radians
     * @param zRad    Z bone rotation in GeckoLib radians
     */
    public static Vec3 rotateEuler(Vec3 v, float xRad, float yRad, float zRad) {
        // Convert bone-space radians to degree-scale used in the original
        float xDeg = ItemRenderUtil.boneRotToDegrees(xRad);
        float yDeg = ItemRenderUtil.boneRotToDegrees(yRad);
        float zDeg = ItemRenderUtil.boneRotToDegrees(zRad);

        double sinX = Math.sin(xDeg * TO_RAD), cosX = Math.cos(xDeg * TO_RAD);
        double sinY = Math.sin(yDeg * TO_RAD), cosY = Math.cos(yDeg * TO_RAD);
        double sinZ = Math.sin(zDeg * TO_RAD), cosZ = Math.cos(zDeg * TO_RAD);

        // Step 1 - rotate around X
        double y1 = v.y * cosX - v.z * sinX;
        double z1 = v.y * sinX + v.z * cosX;

        // Step 2 - rotate around Y
        double x2 = v.x  * cosY + z1 * sinY;
        double z2 = -v.x * sinY + z1 * cosY;

        // Step 3 - rotate around Z
        double x3 = x2 * cosZ - y1 * sinZ;
        double y3 = x2 * sinZ + y1 * cosZ;

        return new Vec3(x3, y3, z2);
    }

    // =========================================================================
    //  Sign-flip helpers
    // =========================================================================

    /**
     * Negates X and Z, leaves Y unchanged.
     *
     * Equivalent to: {@code ck.c(Vec3d)}
     */
    public static Vec3 flipXZ(Vec3 v) {
        return new Vec3(-v.x, v.y, -v.z);
    }

    /**
     * Negates X and Y, leaves Z unchanged.
     *
     * Equivalent to: {@code ck.a(Vec3d)} (single-arg overload)
     */
    public static Vec3 negateXY(Vec3 v) {
        return new Vec3(-v.x, -v.y, v.z);
    }

    /**
     * Negates Y and Z, leaves X unchanged.
     *
     * Equivalent to: {@code ck.b(Vec3d)} (single-arg overload)
     */
    public static Vec3 negateYZ(Vec3 v) {
        return new Vec3(v.x, -v.y, -v.z);
    }

    // =========================================================================
    //  Inverse linear interpolation
    // =========================================================================

    /**
     * Returns the parameter {@code t} such that
     * {@code lerp(min, max, t) == value}, i.e.
     * {@code t = (value - min) / (max - min)}.
     *
     * Equivalent to: {@code ck.a(double min, double max, double value)}
     */
    public static double inverseLerp(double min, double max, double value) {
        return (value - min) / (max - min);
    }

    /**
     * Convenience wrapper: uses the X-components of three {@link Vec3} as
     * min, max, and value for {@link #inverseLerp}.
     *
     * Equivalent to: {@code ck.a(Vec3d a, Vec3d b, Vec3d v)}
     */
    public static double inverseLerpX(Vec3 minVec, Vec3 maxVec, Vec3 valueVec) {
        return inverseLerp(minVec.x, maxVec.x, valueVec.x);
    }
}
