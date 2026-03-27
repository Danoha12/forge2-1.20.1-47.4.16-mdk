package com.trolmastercard.sexmod;

/**
 * IShouldFollowLook (dr) - Ported from 1.12.2 to 1.20.1.
 *
 * Marker interface that entities can implement to expose a boolean
 * {@link #shouldFollowLook()} query.  Used by look-tracking goal logic to
 * decide whether head-following behaviour is currently active.
 *
 * 1.12.2 - 1.20.1 changes:
 *   - Renamed from {@code dr} to {@code IShouldFollowLook} for clarity.
 *   - Single-method interface {@code boolean a()} - {@code shouldFollowLook()}.
 */
public interface IShouldFollowLook {
    /** @return {@code true} if the entity should track and follow player look. */
    boolean shouldFollowLook();
}
