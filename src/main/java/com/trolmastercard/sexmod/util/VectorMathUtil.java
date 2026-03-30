package com.trolmastercard.sexmod.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * VectorMathUtil — Portado a 1.20.1.
 * * Utilidades estáticas para operaciones de vectores (Vec3).
 * * Crucial para el posicionamiento preciso en animaciones y renderizado.
 */
public final class VectorMathUtil {

    private static final double TO_RAD = Math.PI / 180.0D;

    private VectorMathUtil() {}

    // ── Operaciones Básicas ──────────────────────────────────────────────────

    public static Vec3 scale(Vec3 v, double scalar) {
        return new Vec3(v.x * scalar, v.y * scalar, v.z * scalar);
    }

    public static double dot(Vector3f a, Vec3 b) {
        return a.x() * b.x + a.y() * b.y + a.z() * b.z;
    }

    public static double dot(Vec3 a, Vec3 b) {
        return a.x * b.x + a.y * b.y + a.z * b.z;
    }

    public static Vec3 cross(Vec3 a, Vec3 b) {
        return new Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x
        );
    }

    // ── Rotación de Entidad (Yaw/Pitch) ──────────────────────────────────────

    /**
     * Rota un vector basándose en el Yaw y Pitch de la entidad.
     * Mantiene la lógica original del mod para asegurar que los NPCs se alineen bien.
     */
    public static Vec3 rotateYaw(Vec3 v, float xRotDeg, float yawDeg) {
        double xRad = xRotDeg * TO_RAD;
        double yRad = yawDeg  * TO_RAD;

        // Paso 1: Rotación en el eje X (Roll/Pitch)
        double newY = v.y * Math.cos(xRad) - v.z * Math.sin(xRad);
        double newZ = v.y * Math.sin(xRad) + v.z * Math.cos(xRad);
        double newX = v.x;

        // Paso 2: Rotación en el eje Y (Yaw)
        // Minecraft usa un desfase de 90° para que +Z sea Sur
        double outX = -Math.sin((yawDeg + 90.0F) * TO_RAD) * newX - Math.sin(yRad) * newZ;
        double outZ =  Math.cos((yawDeg + 90.0F) * TO_RAD) * newX + Math.cos(yRad) * newZ;

        return new Vec3(outX, newY, outZ);
    }

    public static Vec3 rotateYaw(Vec3 v, float yawDeg) {
        return rotateYaw(v, 0.0F, yawDeg);
    }

    public static Vec3 rotateYaw(double x, double y, double z, float yawDeg) {
        return rotateYaw(new Vec3(x, y, z), yawDeg);
    }

    // ── Rotación de Huesos (Euler X -> Y -> Z) ───────────────────────────────

    /**
     * Aplica rotaciones Euler intrínsecas en orden X -> Y -> Z.
     * Usado principalmente para calcular la posición de los ítems en las manos de los NPCs.
     */
    public static Vec3 rotateEuler(Vec3 v, float xRad, float yRad, float zRad) {
        // Convertimos los radianes de los huesos de GeckoLib a grados según la lógica del mod
        // Nota: Asegúrate de que ItemRenderUtil esté portado
        float xDeg = ItemRenderUtil.boneRotToDegrees(xRad);
        float yDeg = ItemRenderUtil.boneRotToDegrees(yRad);
        float zDeg = ItemRenderUtil.boneRotToDegrees(zRad);

        double sinX = Math.sin(xDeg * TO_RAD), cosX = Math.cos(xDeg * TO_RAD);
        double sinY = Math.sin(yDeg * TO_RAD), cosY = Math.cos(yDeg * TO_RAD);
        double sinZ = Math.sin(zDeg * TO_RAD), cosZ = Math.cos(zDeg * TO_RAD);

        // Rotación en X
        double y1 = v.y * cosX - v.z * sinX;
        double z1 = v.y * sinX + v.z * cosX;

        // Rotación en Y
        double x2 = v.x  * cosY + z1 * sinY;
        double z2 = -v.x * sinY + z1 * cosY;

        // Rotación en Z (z3 es igual a z2 ya que rotamos sobre Z)
        double x3 = x2 * cosZ - y1 * sinZ;
        double y3 = x2 * sinZ + y1 * cosZ;

        return new Vec3(x3, y3, z2);
    }

    // ── Modificadores de Signo ───────────────────────────────────────────────

    public static Vec3 flipXZ(Vec3 v) {
        return new Vec3(-v.x, v.y, -v.z);
    }

    public static Vec3 negateXY(Vec3 v) {
        return new Vec3(-v.x, -v.y, v.z);
    }

    // ── Interpolación ────────────────────────────────────────────────────────

    public static double inverseLerp(double min, double max, double value) {
        if (Math.abs(max - min) < 1.0E-5D) return 0.0D;
        return (value - min) / (max - min);
    }

    public static double inverseLerpX(Vec3 minVec, Vec3 maxVec, Vec3 valueVec) {
        return inverseLerp(minVec.x, maxVec.x, valueVec.x);
    }
}