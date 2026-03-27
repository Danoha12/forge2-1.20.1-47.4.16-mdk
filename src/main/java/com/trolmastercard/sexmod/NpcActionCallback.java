package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;

/**
 * NpcActionCallback - ported from u.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Functional interface used as a callback/listener when an action involving
 * a {@link PlayerKoboldEntity} completes or needs to be triggered.
 *
 * Original obfuscated name: {@code u}, single method {@code a(f_)}.
 * {@code f_} maps to {@link PlayerKoboldEntity}.
 */
@FunctionalInterface
public interface NpcActionCallback {

    /**
     * Called when the action associated with this callback fires.
     *
     * @param npc  the {@link PlayerKoboldEntity} this callback applies to
     */
    void onAction(PlayerKoboldEntity npc);
}
