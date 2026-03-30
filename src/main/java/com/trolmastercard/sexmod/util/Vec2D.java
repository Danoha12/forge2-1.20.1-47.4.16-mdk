package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades

import java.util.Objects;

/**
 * Vec2D — Portado a 1.20.1.
 * * Vector 2D simple de doble precisión (x, z) usado para comparaciones
 * * y cálculos de coordenadas horizontales en los chunks.
 */
public class Vec2D {

    public double x;
    public double z;

    public Vec2D(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public Vec2D subtract(Vec2D other) {
        return new Vec2D(this.x - other.x, this.z - other.z);
    }

    // ── Métodos de Objeto Estándar ───────────────────────────────────────────

    @Override
    public String toString() {
        // Formateado a 2 decimales para que no inunde la consola si lo imprimes
        return String.format("(%.2f, %.2f)", this.x, this.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec2D vec2D = (Vec2D) o;
        return Double.compare(vec2D.x, this.x) == 0 &&
                Double.compare(vec2D.z, this.z) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.z);
    }
}