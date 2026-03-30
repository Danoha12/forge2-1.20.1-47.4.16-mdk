package com.trolmastercard.sexmod.entity; // Sugerencia de carpeta para que esté junto a tus entidades

import com.trolmastercard.sexmod.registry.AnimState;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * NpcStateAccessor — Portado a 1.20.1.
 * * Interfaz que expone los campos de estado compartidos.
 * * Permite que el Renderizador acceda a los datos de la entidad de forma segura.
 */
public interface NpcStateAccessor {

    // ── Pareja de Interacción ────────────────────────────────────────────────

    /** Devuelve el UUID del jugador vinculado, o null si está libre. */
    @Nullable
    UUID getSexPartnerUUID();

    /** Vincula a un jugador/NPC por su UUID. */
    void setSexPartnerUUID(@Nullable UUID partnerUUID);

    // ── Modelo y Ropa ────────────────────────────────────────────────────────

    /** Devuelve el índice de ropa/modelo actual (usado para variantes). */
    int getModelIndex();

    /** Cambia el índice de ropa. */
    void setModelIndex(int index);

    // ── Control de Animación ─────────────────────────────────────────────────

    /** Índice crudo para la máquina de estados de GeckoLib. */
    int getAnimationIndex();

    void setAnimationIndex(int index);

    // ── Contador de Clímax ───────────────────────────────────────────────────

    /** Contador de frames para la animación de clímax/cum. */
    int getCumCounter();

    void setCumCounter(int count);

    // ── Estado de Animación (Enum) ───────────────────────────────────────────

    /** Cambia el estado lógico (ej: de IDLE a START_THROWING). */
    void setAnimState(AnimState state);

    /** Devuelve el estado lógico actual. */
    AnimState getAnimState();
}