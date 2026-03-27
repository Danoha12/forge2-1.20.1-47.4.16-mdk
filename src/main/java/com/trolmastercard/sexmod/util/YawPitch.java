package com.trolmastercard.sexmod.util;

/**
 * YawPitch - ported from bm.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Immutable pair of (yaw, pitch) in radians.
 *
 * Field mapping:
 *   c = yaw   (float, radians)
 *   a = pitch (float, radians)
 *
 * Static constant b = ZERO = (0, 0).
 */
public final class YawPitch {

    public static final YawPitch ZERO = new YawPitch(0.0F, 0.0F);

    /** Yaw in radians. Original field: {@code c}. */
    public final float yaw;

    /** Pitch in radians. Original field: {@code a}. */
    public final float pitch;

    public YawPitch(float yaw, float pitch) {
        this.yaw   = yaw;
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return "YawPitch{yaw=" + yaw + ", pitch=" + pitch + '}';
    }
}
