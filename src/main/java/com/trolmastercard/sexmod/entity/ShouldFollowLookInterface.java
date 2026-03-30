package com.trolmastercard.sexmod.entity; // Te sugiero moverlo al paquete de entidades o IA

/**
 * ShouldFollowLookInterface — Portado a 1.20.1.
 * * Interfaz para entidades o metas (Goals) de IA que siguen
 * * condicionalmente la dirección de la mirada.
 */
@FunctionalInterface
public interface ShouldFollowLookInterface {

    /**
     * @return true si la entidad o la IA debe seguir la dirección de la mirada.
     */
    boolean shouldFollowLook();

}