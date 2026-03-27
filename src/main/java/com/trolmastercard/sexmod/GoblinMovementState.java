package com.trolmastercard.sexmod;

/**
 * GoblinMovementState - ported from ht.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Simple data holder used to track the movement/animation state flags
 * for goblin entities. Stored as a static singleton (ht.a - GoblinMovementState.INSTANCE).
 */
public class GoblinMovementState {

    public static GoblinMovementState INSTANCE;

    /** Whether the goblin is currently attacking. */
    public boolean attacking;
    /** Whether the goblin is currently moving. */
    public boolean moving;
    /** Whether the goblin is currently idle / standing. */
    public boolean idle;

    public GoblinMovementState(boolean attacking, boolean moving, boolean idle) {
        this.attacking = attacking;
        this.moving    = moving;
        this.idle      = idle;
    }
}
