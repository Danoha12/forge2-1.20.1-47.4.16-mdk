package com.trolmastercard.sexmod.util;

import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/**
 * VectorRotateUtil — El compás matemático del mod.
 * Portado a 1.20.1 usando JOML para rotaciones precisas.
 */
public class VectorRotateUtil {

    /**
     * Rota un vector Vec3 en el eje Y (Yaw).
     * Útil para posicionar objetos alrededor de un NPC.
     */
    public static Vec3 rotateY(Vec3 vec, float angle) {
        float rad = angle * (float)Math.PI / 180.0F;
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);
        double x = vec.x * cos + vec.z * sin;
        double z = vec.z * cos - vec.x * sin;
        return new Vec3(x, vec.y, z);
    }

    /**
     * Rota un vector usando ángulos Euler (Pitch, Yaw, Roll).
     */
    public static Vec3 rotate(Vec3 vec, float pitch, float yaw, float roll) {
        Vector3f v = new Vector3f((float)vec.x, (float)vec.y, (float)vec.z);
        Quaternionf quat = new Quaternionf().rotationXYZ(
                pitch * (float)Math.PI / 180.0F,
                yaw * (float)Math.PI / 180.0F,
                roll * (float)Math.PI / 180.0F
        );
        v.rotate(quat);
        return new Vec3(v.x(), v.y(), v.z());
    }

    /**
     * Versión para Vector3f (usada internamente por GeckoLib/Renderers).
     */
    public static Vector3f rotateVector(Vector3f vec, float pitch, float yaw, float roll) {
        Quaternionf quat = new Quaternionf().rotationXYZ(
                pitch * (float)Math.PI / 180.0F,
                yaw * (float)Math.PI / 180.0F,
                roll * (float)Math.PI / 180.0F
        );
        return vec.rotate(quat);
    }
}