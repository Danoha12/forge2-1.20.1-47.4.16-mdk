package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.GalathEntity;

/**
 * GalathPredicate — Portado a 1.20.1.
 * * Interfaz funcional de uso general para evaluar condiciones lógicas
 * relacionadas con GalathEntity.
 */
@FunctionalInterface
public interface GalathPredicate {
    /**
     * Evalúa una condición lógica sobre la instancia de Galath.
     * * @param galath La entidad a evaluar.
     * @return true si la condición se cumple, false en caso contrario.
     */
    boolean test(GalathEntity galath);
}