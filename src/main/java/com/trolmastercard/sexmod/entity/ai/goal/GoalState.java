package com.trolmastercard.sexmod.entity.ai.goal;

/**
 * GoalState — El enumerador universal para los estados de la IA.
 * Unifica los estados de combate, seguimiento y descanso.
 */
public enum GoalState {
    IDLE,
    FOLLOW,
    ATTACK,
    RIDE,
    DOWNED,
    BOW // Añadido por si el Kobold usa arco
}