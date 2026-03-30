package com.trolmastercard.sexmod.client.model; // Sugiero este paquete si es solo para renderizado

/**
 * NpcGirlInterface — Portado a 1.20.1.
 * * Interfaz que expone los nombres de los "huesos" (bones) del modelo 3D.
 * * Usado por el renderizador (probablemente GeckoLib) para ocultar/mostrar
 * * partes del cuerpo o ropa (faldas, cabello, etc.).
 */
public interface NpcGirlInterface {

    /** Categoría C (ej. parte delantera de la falda). */
    default String[] getBonesC() { return new String[0]; }

    /** Categoría G (ej. parte trasera de la falda). */
    default String[] getBonesG() { return new String[0]; }

    /** Categoría F (ej. pechos). */
    default String[] getBonesF() { return new String[0]; }

    /** Categoría A (ej. cabello). */
    default String[] getBonesA() { return new String[0]; }

    /** Categoría H (ej. glúteos). */
    default String[] getBonesH() { return new String[0]; }

    /** Categoría E (ej. muslos). */
    default String[] getBonesE() { return new String[0]; }

    /** Categoría B (ej. brazos). */
    default String[] getBonesB() { return new String[0]; }

    /** Categoría D (ej. piernas). */
    default String[] getBonesD() { return new String[0]; }
}