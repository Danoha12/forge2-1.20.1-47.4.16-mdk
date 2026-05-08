package com.trolmastercard.sexmod.util;

import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * RgbaColor — EL UNIFICADO (Versión Final con Compatibilidad).
 */
public final class RgbaColor {

    public int r, g, b, a;

    public static final RgbaColor ZERO = new RgbaColor(0, 0, 0, 0);
    public static final RgbaColor WHITE = new RgbaColor(255, 255, 255, 255);

    public RgbaColor(int r, int g, int b, int a) {
        this.r = Mth.clamp(r, -1000, 1000); // Dejamos más margen por si el modelo usa valores locos
        this.g = Mth.clamp(g, -1000, 1000);
        this.b = Mth.clamp(b, -1000, 1000);
        this.a = Mth.clamp(a, 0, 255);
    }

    public RgbaColor(int r, int g, int b) {
        this(r, g, b, 255);
    }

    // 🚨 LA PIEZA DE COMPATIBILIDAD: Para que el modelo no chille por los paréntesis
    public int r() { return this.r; }
    public int g() { return this.g; }
    public int b() { return this.b; }
    public int a() { return this.a; }

    public int pack() {
        return FastColor.ARGB32.color(this.a, this.r, this.g, this.b);
    }

    // -- Matemáticas --
    public static RgbaColor lerp(RgbaColor start, RgbaColor end, float pct) {
        return new RgbaColor(
                (int) Mth.lerp(pct, start.r, end.r),
                (int) Mth.lerp(pct, start.g, end.g),
                (int) Mth.lerp(pct, start.b, end.b),
                (int) Mth.lerp(pct, start.a, end.a)
        );
    }

    // Restar, sumar y escalar (para las físicas de Galath)
    public RgbaColor subtract(RgbaColor other) {
        return new RgbaColor(this.r - other.r, this.g - other.g, this.b - other.b, this.a);
    }

    public RgbaColor add(RgbaColor other) {
        return new RgbaColor(this.r + other.r, this.g + other.g, this.b + other.b, this.a);
    }

    public RgbaColor scale(float scale) {
        return new RgbaColor((int)(this.r * scale), (int)(this.g * scale), (int)(this.b * scale), this.a);
    }
}