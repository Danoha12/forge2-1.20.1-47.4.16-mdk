package com.trolmastercard.sexmod.util;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * MathUtil — Portado a 1.20.1.
 * * Proporciona funciones de interpolación, easing y manejo de ángulos.
 * * Esencial para que las animaciones de GeckoLib y la cámara se vean fluidas.
 */
public final class MathUtil {

    private MathUtil() {}

    // ── Interpolación Básica (LERP) ──────────────────────────────────────────

    public static double lerp(double a, double b, double t) {
        return Mth.lerp(t, a, b);
    }

    public static float lerp(float a, float b, float t) {
        return Mth.lerp(t, a, b);
    }

    /** Interpolación suave de posición Vec3 */
    public static Vec3 lerpPosition(Vec3 from, Vec3 to, double t) {
        return new Vec3(
                Mth.lerp(t, from.x, to.x),
                Mth.lerp(t, from.y, to.y),
                Mth.lerp(t, from.z, to.z)
        );
    }

    // ── Manejo de Ángulos y Yaw ──────────────────────────────────────────────

    /** Normaliza un ángulo a un rango de [-PI, PI] */
    public static float normalizeAngle(float angle) {
        while (angle <= -(float)Math.PI) angle += (float)Math.PI * 2;
        while (angle > (float)Math.PI) angle -= (float)Math.PI * 2;
        return angle;
    }

    /** Interpolación de ángulos en radianes con envoltura circular */
    public static float lerpAngle(float from, float to, double t) {
        float delta = normalizeAngle(to - from);
        return (float)(from + delta * t);
    }

    /** Interpolación de ángulos en grados (usado para la rotación del cuerpo) */
    public static float lerpYaw(float current, float prev, float partialTick) {
        return Mth.lerpYaw(partialTick, prev, current);
    }

    public static float lerpAngleDeg(float from, float to, double t) {
        return Mth.lerpAngle((float)t, from, to);
    }

    // ── Easing Functions (El "Feeling" de la animación) ──────────────────────

    /** Suavizado de salida (acelera al inicio, frena al final) */
    public static double easeOutQuart(double t) {
        return 1.0 - Math.pow(1.0 - t, 4.0);
    }

    public static double easeOutCubic(double t) {
        return 1.0 - Math.pow(1.0 - t, 3.0);
    }

    /** Efecto de rebote al final (Back out) */
    public static double easeOutBack(double t) {
        final double c1 = 1.70158, c2 = c1 + 1.0;
        return 1.0 + c2 * Math.pow(t - 1.0, 3.0) + c1 * Math.pow(t - 1.0, 2.0);
    }

    public static double easeInBack(double t) {
        final double c1 = 1.70158, c2 = c1 + 1.0;
        return c2 * t * t * t - c1 * t * t;
    }

    /** Curva sinusoidal (muy orgánica para respiración o balanceo) */
    public static double easeInSine(double t) {
        return Math.sin(t * Math.PI / 2.0);
    }

    public static double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1.0) / 2.0;
    }

    // ── Utilidades de Vectores ───────────────────────────────────────────────

    public static Vec3 stepTowards(Vec3 from, Vec3 to, int steps) {
        if (steps <= 0) return to;
        Vec3 delta = to.subtract(from);
        return from.add(delta.scale(1.0 / steps));
    }

    public static Vec3i lerpVec3i(Vec3i from, Vec3i to, double t) {
        return new Vec3i(
                (int)Mth.lerp(t, from.getX(), to.getX()),
                (int)Mth.lerp(t, from.getY(), to.getY()),
                (int)Mth.lerp(t, from.getZ(), to.getZ())
        );
    }

    /** Interpolación de color RGBA (arreglos de 4 ints) */
    public static int[] lerpRgba(int[] from, int[] to, double t) {
        return new int[]{
                (int)Mth.lerp(t, from[0], to[0]),
                (int)Mth.lerp(t, from[1], to[1]),
                (int)Mth.lerp(t, from[2], to[2]),
                (int)Mth.lerp(t, from[3], to[3])
        };
    }
}