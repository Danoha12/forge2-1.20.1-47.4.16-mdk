package com.trolmastercard.sexmod.util;

/**
 * TickableCallback — Portado a 1.20.1.
 * * Interfaz funcional para ejecutar código en cada tick del juego.
 * * Original: bh.class (Fapcraft 1.12.2)
 */
@FunctionalInterface
public interface TickableCallback {
    /**
     * Método que se ejecuta una vez por cada tick del servidor/cliente (20 veces por segundo).
     */
    void tick();
}