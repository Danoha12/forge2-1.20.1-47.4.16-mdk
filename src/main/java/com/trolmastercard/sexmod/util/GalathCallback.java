package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.GalathEntity;

/**
 * GalathCallback — Portado a 1.20.1.
 * * Interfaz funcional genérica para ejecutar bloques de código
 * que requieren una instancia de GalathEntity.
 */
@FunctionalInterface
public interface GalathCallback {
    /**
     * Ejecuta la lógica personalizada.
     * * @param galath La instancia de la entidad sobre la que se opera.
     */
    void execute(GalathEntity galath);
}