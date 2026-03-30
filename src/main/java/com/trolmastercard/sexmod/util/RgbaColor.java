package com.trolmastercard.sexmod.util;

import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * RgbaColor — Portado a 1.20.1.
 * * Contenedor mutable para colores RGBA.
 * * Reemplaza la clase ofuscada 'bl' de la 1.12.2.
 */
public final class RgbaColor {

    public int r;
    public int g;
    public int b;
    public int a;

    public RgbaColor(int r, int g, int b, int a) {
        this.r = Mth.clamp(r, 0, 255);
        this.g = Mth.clamp(g, 0, 255);
        this.b = Mth.clamp(b, 0, 255);
        this.a = Mth.clamp(a, 0, 255);
    }

    /**
     * Empaqueta el color en un entero de 32 bits (ARGB).
     * Formato estándar usado por los VertexConsumers de la 1.20.1.
     */
    public int pack() {
        return FastColor.ARGB32.color(this.a, this.r, this.g, this.b);
    }

    // ── Helpers para Renderizado (0.0F - 1.0F) ───────────────────────────────

    public float getRedFloat()   { return this.r / 255.0F; }
    public float getGreenFloat() { return this.g / 255.0F; }
    public float getBlueFloat()  { return this.b / 255.0F; }
    public float getAlphaFloat() { return this.a / 255.0F; }

    /** Crea una copia de este color */
    public RgbaColor copy() {
        return new RgbaColor(this.r, this.g, this.b, this.a);
    }

    @Override
    public String toString() {
        return String.format("RGBA(%d, %d, %d, %d)", r, g, b, a);
    }
}