package com.trolmastercard.sexmod.entity;

/**
 * Resettable - Portado de bh.class (1.12.2) a 1.20.1.
 * Implementada por entidades que requieren volver a un estado limpio/default.
 */
public interface Resettable {

    /**
     * Restablece el objeto a su estado inicial.
     * Original: void b()
     */
    void reset();
}