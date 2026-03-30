package com.trolmastercard.sexmod.util;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

/**
 * GoblinColor — Portado a 1.20.1.
 * * Variantes de color de piel para los NPCs Goblin.
 * * Usado en el segmento [7] del "Model Code" genético.
 */
public enum GoblinColor {

    PURPLE(103,  39, 123),
    ORANGE(251, 153,  56),
    BLACK ( 30,  33,  38),
    BLUE  ( 88,  83, 186),
    BROWN ( 63,  35,  34),
    PINK  (247, 102, 109),
    RED   (241,  69,  49),
    GREEN ( 75, 143, 106);

    private final Vec3i rgb;
    private final Vec3 colorVec;

    GoblinColor(int r, int g, int b) {
        this.rgb = new Vec3i(r, g, b);
        // Pre-calculamos el Vec3 (que usa doubles) porque el renderizador lo pide constantemente
        this.colorVec = new Vec3(r, g, b);
    }

    public Vec3i getRgb() {
        return this.rgb;
    }

    /**
     * Devuelve el color en formato Vec3.
     * Requerido por el GoblinEntityRenderer para teñir los huesos de GeckoLib.
     */
    public Vec3 getColorVec() {
        return this.colorVec;
    }

    /** * Devuelve el índice (ordinal) del color.
     * Optimizado para usar el método nativo de Java en lugar de un bucle.
     */
    public static int indexOf(GoblinColor color) {
        return color != null ? color.ordinal() : 0;
    }

    /**
     * Obtiene un color seguro basado en un índice (útil al leer NBT o Model Codes).
     */
    public static GoblinColor getByIndex(int index) {
        GoblinColor[] values = values();
        return values[Math.max(0, Math.min(index, values.length - 1))];
    }
}