package com.trolmastercard.sexmod.entity; // O el paquete donde guardes tus interfaces

/**
 * IShouldFollowLook — Portado a 1.20.1.
 * * Interfaz para entidades que pueden decidir dinámicamente si siguen la mirada del jugador.
 */
public interface IShouldFollowLook {

    /** @return true si la entidad debe rastrear y seguir la mirada del jugador. */
    boolean shouldFollowLook();
}