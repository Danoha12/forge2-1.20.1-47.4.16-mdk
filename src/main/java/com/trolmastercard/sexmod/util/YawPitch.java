package com.trolmastercard.sexmod.util;

import java.util.Objects;

/**
 * YawPitch — Portado a 1.20.1.
 * * Maneja los ángulos de rotación actuales y del frame anterior (Prev).
 * * Vital para suavizar animaciones mediante interpolación (LERP).
 */
public final class YawPitch {

    public static final YawPitch ZERO = new YawPitch(0.0F, 0.0F);

    // Campos públicos para acceso rápido desde los modelos
    public float yaw;
    public float pitch;

    // Campos de interpolación (Frame anterior)
    public float yawPrev;
    public float pitchPrev;

    public YawPitch(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
        // Al nacer, el frame anterior es igual al actual
        this.yawPrev = yaw;
        this.pitchPrev = pitch;
    }

    /**
     * Guarda los valores actuales en los campos 'Prev'.
     * ¡Llamar a este método al final del tick de la entidad!
     */
    public void updatePrev() {
        this.yawPrev = this.yaw;
        this.pitchPrev = this.pitch;
    }

    // ── Métodos de Objeto Estándar ───────────────────────────────────────────

    @Override
    public String toString() {
        return "YawPitch{yaw=" + this.yaw + ", pitch=" + this.pitch + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YawPitch yawPitch = (YawPitch) o;
        return Float.compare(yawPitch.yaw, this.yaw) == 0 &&
                Float.compare(yawPitch.pitch, this.pitch) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.yaw, this.pitch);
    }
    // Constructor para capturar el frame actual y el anterior (Prev) de golpe
    public YawPitch(float yaw, float pitch, float yawPrev, float pitchPrev) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.yawPrev = yawPrev;
        this.pitchPrev = pitchPrev;
    }
}