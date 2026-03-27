package com.trolmastercard.sexmod.entity;

import java.util.UUID;

/**
 * GalathSexEntity - Interfaz de Coordinación de Interacciones.
 * Portado a 1.20.1.
 * * Esta interfaz permite que las entidades NPC se comuniquen con el sistema
 * de animaciones sincronizadas y secuencias de interacción.
 */
public interface GalathSexEntity {

    /**
     * Inicia la secuencia de aproximación para la interacción (ej. caminar hacia la cama).
     * Mantenemos el nombre original para compatibilidad con StartGalathSexPacket.
     */
    default void startSexApproach() {
        // Implementado por cada NPC (Jenny, Ellie, etc.)
    }

    /**
     * Callback disparado cuando la interacción comienza formalmente.
     */
    void onGalathSexStart();

    /**
     * Versión extendida de la aproximación con parámetros de control.
     */
    default void startSexApproach(boolean active, boolean leader, UUID playerUUID) {
        // Implementado opcionalmente por la entidad
    }
}