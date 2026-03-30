package com.trolmastercard.sexmod.entity; // Ajusta al paquete de tus entidades/IA

import javax.annotation.Nullable;
// Si KoboldEntity está en otro paquete, asegúrate de importarlo aquí
// import com.trolmastercard.sexmod.entity.KoboldEntity;

/**
 * NpcQueryInterface — Portado a 1.20.1.
 * * Expone los estados booleanos y el objetivo de combate para los NPCs
 * * que participan en las mecánicas de la tribu o secuencias de ataque.
 */
public interface NpcQueryInterface {

    /** Devuelve el objetivo de combate actual, o null si está pacífico. */
    @Nullable
    KoboldEntity getCombatTarget();

    /** Devuelve true si el NPC está en modo defensa/guardia de la tribu. */
    boolean isDefending();

    /** Devuelve true si este NPC está en alerta máxima. */
    boolean isAlarmed();

    /** Devuelve true si el NPC está atacando activamente. */
    boolean isAttacking();
}