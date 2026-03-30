package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades

import java.util.Objects;

/**
 * Vec2i — Portado a 1.20.1.
 * * Par simple de coordenadas enteras 2D.
 * * 🚨 ADVERTENCIA: Las variables x e y son mutables. ¡NO modifiques Vec2i.ZERO!
 */
public class Vec2i {

    public static final Vec2i ZERO = new Vec2i(0, 0);

    public int x;
    public int y;

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Distancia euclidiana desde este punto hasta (toX, toY).
     */
    public float distanceTo(int toX, int toY) {
        // 🧮 Math.hypot es más seguro y rápido que hacer la raíz cuadrada manual
        return (float) Math.hypot(toX - this.x, toY - this.y);
    }

    // ── Métodos de Objeto Estándar ───────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("(%d, %d)", this.x, this.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec2i vec2i = (Vec2i) o;
        return this.x == vec2i.x && this.y == vec2i.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y);
    }
}