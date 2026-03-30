package com.trolmastercard.sexmod.world.gen; // Te sugiero moverlo a un paquete de generación de mundo

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource; // 🚨 El estándar de 1.20.1
import net.minecraft.world.level.Level;

/**
 * SexmodStructure — Portado a 1.20.1.
 * * Interfaz marcadora para todos los generadores de estructuras basadas en NBT del mod.
 * * Define el contrato básico para colocar estructuras en el mundo.
 */
public interface SexmodStructure {

    /**
     * Intenta generar la estructura en la posición dada.
     *
     * @param level  El nivel (mundo) donde se colocará.
     * @param random Fuente de aleatoriedad moderna de Minecraft.
     * @param pos    La posición central o de pivote de la estructura.
     * @return true si la colocación fue exitosa, false si falló o fue bloqueada.
     */
    boolean generate(Level level, RandomSource random, BlockPos pos);

}