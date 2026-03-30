package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades

/**
 * RgbColor — Portado a 1.20.1.
 * * Tripleta simple de color RGB (floats de 0.0 a 1.0).
 * * (Pro-tip: En Minecraft moderno, org.joml.Vector3f hace esto de forma nativa).
 */
public class RgbColor {

    public static final RgbColor BLACK = new RgbColor(0.0F, 0.0F, 0.0F);

    /** Canal Rojo [0..1] */
    public float r;
    /** Canal Verde [0..1] */
    public float g;
    /** Canal Azul [0..1] */
    public float b;

    public RgbColor(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /** Devuelve la resta de los componentes (this - other). */
    public RgbColor subtract(RgbColor other) {
        return new RgbColor(this.r - other.r, this.g - other.g, this.b - other.b);
    }

    /** Devuelve la suma de los componentes (this + other). */
    public RgbColor add(RgbColor other) {
        return new RgbColor(this.r + other.r, this.g + other.g, this.b + other.b);
    }

    /** Escala los componentes por un multiplicador (this * scale). */
    public RgbColor scale(float scale) {
        return new RgbColor(this.r * scale, this.g * scale, this.b * scale);
    }
}