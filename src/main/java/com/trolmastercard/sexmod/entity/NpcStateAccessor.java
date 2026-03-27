package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * NpcStateAccessor - Interfaz de Acceso de Estado de NPC.
 * Portado a 1.20.1.
 * * Expone los campos de estado de animación compartidos entre BaseNpcEntity
 * y sus subclases para coordinar interacciones complejas.
 */
public interface NpcStateAccessor {

    // =========================================================================
    //  Objetivo de Interacción
    // =========================================================================

    /** * Devuelve el UUID del jugador/NPC actualmente interactuando de forma cercana con esta entidad.
     * @return UUID del objetivo, o null si está inactiva.
     */
    @Nullable
    UUID getInteractionTargetUUID();

    /** * Establece el UUID del objetivo de interacción. Pasa null para limpiar y terminar el evento.
     */
    void setInteractionTargetUUID(@Nullable UUID partnerUUID);

    // =========================================================================
    //  Modelos y Apariencia
    // =========================================================================

    /** Devuelve el índice de ropa/modelo actual. */
    int getModelIndex();

    /** Establece el índice de ropa/modelo. */
    void setModelIndex(int index);

    // =========================================================================
    //  Índices de Animación
    // =========================================================================

    /** Devuelve el índice numérico crudo usado internamente por la máquina de estados. */
    int getAnimationIndex();

    /** Establece el índice numérico crudo de la animación actual. */
    void setAnimationIndex(int index);

    // =========================================================================
    //  Contador de Finalización de Evento
    // =========================================================================

    /** * Devuelve el contador de frames para la etapa final de la interacción (clímax/resolución).
     */
    int getFinishCounter();

    /** * Establece el contador de frames de la etapa de resolución.
     */
    void setFinishCounter(int count);

    // =========================================================================
    //  Estado de Animación Principal (AnimState)
    // =========================================================================

    /** Establece el estado de animación de alto nivel del diccionario AnimState. */
    void setAnimState(AnimState state);

    /** Devuelve el estado actual de animación de alto nivel. */
    AnimState getAnimState();
}