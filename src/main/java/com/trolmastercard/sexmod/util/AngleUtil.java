package com.trolmastercard.sexmod.util;

import net.minecraft.world.phys.Vec3;

/**
 * Static helpers for angle arithmetic (pitch from two points, yaw normalisation, etc.).
 * Obfuscated name: gc
 */
public class AngleUtil {

    /** Pitch in radians from {@code from} looking toward {@code to}. */
    public static double pitchRad(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        return Math.atan2(dz, Math.sqrt(dx * dx + dy * dy));
    }

    /** Normalises a float yaw to [0, 360). */
    public static float normalizeYaw(float yaw) {
        yaw %= 360.0F;
        if (yaw < 0.0F) yaw += 360.0F;
        return yaw;
    }

    /** Normalises a float angle to [0, 360). */
    public static float normalizeAngleF(float angle) {
        angle %= 360.0F;
        return (angle >= 0.0F) ? angle : (angle + 360.0F);
    }

    /** Normalises a double angle to [0, 360). */
    public static double normalizeAngleD(double angle) {
        angle %= 360.0D;
        return (angle >= 0.0D) ? angle : (angle + 360.0D);
    }

    /** Converts a period in degrees to angular velocity in rad/tick (2- / 360 / period). */
    public static float angularVelocityFromPeriod(float periodDegrees) {
        return (float)(Math.PI * 2.0D / 360.0D / periodDegrees);
    }

    /** Double overload of {@link #angularVelocityFromPeriod(float)}. */
    public static float angularVelocityFromPeriod(double periodDegrees) {
        return (float)(Math.PI * 2.0D / 360.0D / periodDegrees);
    }

    /** Converts radians to degrees. */
    public static float toDegrees(float radians) {
        return (float)(57.29577951308232D * radians);
    }

    /** Converts radians to degrees (double version). */
    public static double toDegrees(double radians) {
        return 57.29577951308232D * radians;
    }
}
