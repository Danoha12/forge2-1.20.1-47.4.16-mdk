package com.trolmastercard.sexmod.util;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/**
 * EyeColor — Portado a 1.20.1.
 * * Mapea las variantes de color de ojos a vectores RGB.
 * * Usado en el segmento [8] del "Model Code".
 */
public enum EyeColor {

    RED   (255,   0,   0),
    VIOLET(132,  30, 156),
    YELLOW(243, 247,   0),
    BROWN (105,  60,   9),
    TURKEY(  0, 206, 217), // Turquoise (Turquesa) 🦃
    BLUE  (  0,   0, 255);

    private final Vec3i rgb;
    private final Vec3 colorVec;

    EyeColor(int r, int g, int b) {
        // Guardamos los enteros exactos por si los necesitas en alguna UI
        this.rgb = new Vec3i(r, g, b);
        // Pre-calculamos el Vec3 (doubles) para el GoblinEntityRenderer
        this.colorVec = new Vec3(r, g, b);
    }

    public Vec3 getColorVec() {
        return this.colorVec;
    }

    public int[] getRGB() {
        return new int[]{ this.rgb.getX(), this.rgb.getY(), this.rgb.getZ() };
    }

    /**
     * Búsqueda inversa a partir de un vector.
     */
    public static EyeColor fromVec(Vec3 v) {
        if (v == null) return RED;
        for (EyeColor c : values()) {
            if (c.colorVec.equals(v)) return c;
        }
        return RED; // Fallback por defecto
    }

    /**
     * Devuelve el índice (ordinal) del color.
     * Optimizado para O(1) eliminando el bucle for original.
     */
    public static int indexOf(EyeColor color) {
        return color != null ? color.ordinal() : 0;
    }

    /**
     * Obtiene un color seguro basado en su índice (ideal para leer el Model Code).
     */
    public static EyeColor getByIndex(int index) {
        EyeColor[] values = values();
        return values[Math.max(0, Math.min(index, values.length - 1))];
    }
}