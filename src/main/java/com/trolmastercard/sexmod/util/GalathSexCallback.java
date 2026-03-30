package com.trolmastercard.sexmod.util;

/**
 * GalathSexCallback — Portado a 1.20.1.
 * * Interfaz funcional (Callback) invocada cuando un evento de interacción
 * con Galath se activa o se completa.
 */
@FunctionalInterface
public interface GalathSexCallback {
    /**
     * Ejecuta la lógica programada al finalizar o disparar el evento.
     */
    void onSexEvent();
}