package com.trolmastercard.sexmod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
/**
 * HairColor — El catálogo de tintes para Goblins.
 * Define los colores disponibles para el pelo en el sistema de ModelCode.
 */
public enum HairColor {
    BLACK(0.1F, 0.1F, 0.1F),
    BLONDE(0.9F, 0.8F, 0.3F),
    BROWN(0.4F, 0.2F, 0.1F),
    RED(0.7F, 0.1F, 0.1F),
    WHITE(0.95F, 0.95F, 0.95F),
    GRAY(0.5F, 0.5F, 0.5F),
    BLUE(0.2F, 0.3F, 0.8F),
    PINK(0.9F, 0.4F, 0.7F);

    private final float r, g, b;

    HairColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }

    /**
     * Obtiene el color por índice (usado por el decodificador de ModelCode).
     */
    public static HairColor byIndex(int index) {
        HairColor[] values = values();
        return values[Mth.clamp(index, 0, values.length - 1)];
    }
    public Vec3 getColorVec() {
        // Multiplicamos por 255 porque tu Renderer divide por 255 después
        return new Vec3(this.r * 255.0D, this.g * 255.0D, this.b * 255.0D);
    }
}