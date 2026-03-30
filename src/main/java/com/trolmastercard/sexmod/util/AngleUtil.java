package com.trolmastercard.sexmod.util;

import net.minecraft.world.phys.Vec3;

/**
 * AngleUtil — Portado a 1.20.1 y enmascarado (SFW).
 *
 * Funciones estáticas de ayuda para la aritmética de ángulos
 * (cálculo de pitch entre dos puntos, normalización de yaw, conversión de radianes, etc.).
 */
public class AngleUtil {

    /** Calcula el pitch en radianes desde el punto 'from' mirando hacia 'to'. */
    public static double pitchRad(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        return Math.atan2(dz, Math.sqrt(dx * dx + dy * dy));
    }

    /** Normaliza un yaw en formato float al rango [0, 360). */
    public static float normalizeYaw(float yaw) {
        yaw %= 360.0F;
        if (yaw < 0.0F) yaw += 360.0F;
        return yaw;
    }

    /** Normaliza un ángulo en formato float al rango [0, 360). */
    public static float normalizeAngleF(float angle) {
        angle %= 360.0F;
        return (angle >= 0.0F) ? angle : (angle + 360.0F);
    }

    /** Normaliza un ángulo en formato double al rango [0, 360). */
    public static double normalizeAngleD(double angle) {
        angle %= 360.0D;
        return (angle >= 0.0D) ? angle : (angle + 360.0D);
    }

    /** Convierte un periodo en grados a velocidad angular en rad/tick (2π / 360 / periodo). */
    public static float angularVelocityFromPeriod(float periodDegrees) {
        return (float)(Math.PI * 2.0D / 360.0D / periodDegrees);
    }

    /** Sobrecarga en double de {@link #angularVelocityFromPeriod(float)}. */
    public static float angularVelocityFromPeriod(double periodDegrees) {
        return (float)(Math.PI * 2.0D / 360.0D / periodDegrees);
    }

    /** Convierte radianes a grados. */
    public static float toDegrees(float radians) {
        return (float)(57.29577951308232D * radians);
    }

    /** Convierte radianes a grados (versión double). */
    public static double toDegrees(double radians) {
        return 57.29577951308232D * radians;
    }
}