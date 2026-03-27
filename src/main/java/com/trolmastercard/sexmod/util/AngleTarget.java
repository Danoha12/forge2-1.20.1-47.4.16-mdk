package com.trolmastercard.sexmod.util;

/**
 * AngleTarget - ported from bm.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Simple value-object holding yaw + pitch (both in radians).
 * Returned by {@link LightUtil#getHeadingTo}.
 *
 * Field mapping (confirmed from source):
 *   c - yaw   (first  constructor arg)
 *   a - pitch (second constructor arg)
 *
 * Static constant: {@code b = new bm(0, 0)} - {@link #ZERO}
 */
public final class AngleTarget {

    /** Identity (both angles zero). Original: {@code static final bm b = new bm(0, 0)}. */
    public static final AngleTarget ZERO = new AngleTarget(0.0F, 0.0F);

    /** Yaw in radians. Original field: {@code c}. */
    public final float yaw;

    /** Pitch in radians. Original field: {@code a}. */
    public final float pitch;

    public AngleTarget(float yaw, float pitch) {
        this.yaw   = yaw;
        this.pitch = pitch;
    }
}
