package com.trolmastercard.sexmod.util; // O el paquete donde tengas tus utilidades / interfaces

import com.trolmastercard.sexmod.entity.BaseNpcEntity;

/**
 * NpcFloatSupplier — Portado a 1.20.1.
 * * Interfaz funcional para obtener un valor float dinámico de un NPC.
 * * Usado probablemente para cálculos de animación, físicas o renderizado.
 */
@FunctionalInterface
public interface NpcFloatSupplier {
    float get(BaseNpcEntity npc);
}