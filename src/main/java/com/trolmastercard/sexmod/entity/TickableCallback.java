package com.trolmastercard.sexmod.entity;

/**
 * TickableCallback - Portado de bh.class (1.12.2) a 1.20.1.
 * Interfaz de un solo método para callbacks de ticks.
 */
@FunctionalInterface
public interface TickableCallback {
    void tick();
}