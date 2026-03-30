package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades (o math)

import java.util.Objects;

/**
 * YawPitch — Portado a 1.20.1.
 * * Par inmutable de (yaw, pitch) en radianes.
 * * * * Nota del herrero: Mantenido como 'class' en lugar de 'record' (Java 17)
 * * para no romper la compatibilidad de acceso directo a los campos públicos (.yaw / .pitch).
 */
public final class YawPitch {

    public static final YawPitch ZERO = new YawPitch(0.0F, 0.0F);

    /** Yaw en radianes. */
    public final float yaw;

    /** Pitch en radianes. */
    public final float pitch;

    public YawPitch(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
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
}