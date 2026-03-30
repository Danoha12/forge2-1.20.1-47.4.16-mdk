package com.trolmastercard.sexmod.util;

/**
 * AngleTarget — Portado a 1.20.1 y enmascarado (SFW).
 *
 * Objeto de valor simple que contiene yaw + pitch (ambos en radianes).
 * Usado comúnmente como retorno por {@link LightUtil#getHeadingTo}.
 */
public final class AngleTarget {

    /** Identidad (ambos ángulos en cero). */
    public static final AngleTarget ZERO = new AngleTarget(0.0F, 0.0F);

    /** Yaw (Rotación horizontal) en radianes. */
    public final float yaw;

    /** Pitch (Inclinación vertical) en radianes. */
    public final float pitch;

    public AngleTarget(float yaw, float pitch) {
        this.yaw   = yaw;
        this.pitch = pitch;
    }
}