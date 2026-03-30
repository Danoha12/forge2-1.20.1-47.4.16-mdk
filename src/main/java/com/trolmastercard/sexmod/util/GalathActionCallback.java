package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.GalathEntity;

/**
 * GalathActionCallback — Portado a 1.20.1.
 * * Interfaz funcional utilizada para ejecutar acciones diferidas sobre Galath.
 * * Permite el uso de Lambdas en métodos de utilidad como ModUtil.scheduleDelay.
 */
@FunctionalInterface
public interface GalathActionCallback {
    /**
     * Ejecuta la lógica personalizada sobre la instancia de Galath proporcionada.
     * * @param galath La entidad sobre la que se aplica la acción.
     */
    void execute(GalathEntity galath);
}