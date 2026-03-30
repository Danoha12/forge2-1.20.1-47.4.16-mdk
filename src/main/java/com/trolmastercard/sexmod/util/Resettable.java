package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades o interfaces

/**
 * Resettable — Portado a 1.20.1.
 * * Interfaz de un solo método implementada por entidades o sistemas
 * * que soportan ser devueltos a un estado limpio o por defecto.
 */
@FunctionalInterface
public interface Resettable {

    /**
     * Reinicia este objeto a su estado por defecto.
     */
    void reset();
}