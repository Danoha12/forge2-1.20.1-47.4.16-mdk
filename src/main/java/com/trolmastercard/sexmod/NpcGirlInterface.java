package com.trolmastercard.sexmod;

/**
 * NpcGirlInterface - exposes bone-name arrays for girl-specific NPC rendering.
 * Ported from gs.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 */
public interface NpcGirlInterface {

    /** Bone names for category c (e.g. skirt front). */
    default String[] getBonesC() { return new String[0]; }

    /** Bone names for category g (e.g. skirt back). */
    default String[] getBonesG() { return new String[0]; }

    /** Bone names for category f (e.g. boobs). */
    default String[] getBonesF() { return new String[0]; }

    /** Bone names for category a (e.g. hair). */
    default String[] getBonesA() { return new String[0]; }

    /** Bone names for category h (e.g. butt). */
    default String[] getBonesH() { return new String[0]; }

    /** Bone names for category e (e.g. thighs). */
    default String[] getBonesE() { return new String[0]; }

    /** Bone names for category b (e.g. arms). */
    default String[] getBonesB() { return new String[0]; }

    /** Bone names for category d (e.g. legs). */
    default String[] getBonesD() { return new String[0]; }
}
