package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.GalathEntity;

/**
 * GalathAttackPredicate — Portado a 1.20.1.
 * * Interfaz funcional utilizada para evaluar si Galath puede realizar un ataque.
 * * Se utiliza principalmente en el enum GalathAttackState para filtrar ataques válidos.
 */
@FunctionalInterface
public interface GalathAttackPredicate {
    /**
     * Evalúa una condición de combate sobre la instancia de Galath.
     * * @param galath La entidad que intenta atacar.
     * @return true si se cumplen las condiciones para el ataque, false en caso contrario.
     */
    boolean test(GalathEntity galath);
}