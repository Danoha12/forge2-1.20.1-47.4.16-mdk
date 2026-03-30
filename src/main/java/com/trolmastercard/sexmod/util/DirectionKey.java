package com.trolmastercard.sexmod.util;

/**
 * DirectionKey — Portado a 1.20.1.
 * * Representa las teclas de movimiento WASD para interacciones y vuelo.
 */
public enum DirectionKey {
    W, A, S, D;

    /** Convierte un índice (int) de vuelta al Enum. Útil para paquetes de red. */
    public static DirectionKey fromInt(int index) {
        if (index < 0 || index >= values().length) return W;
        return values()[index];
    }

    /** Devuelve el índice del enum. */
    public int getIndex() {
        return this.ordinal();
    }
}