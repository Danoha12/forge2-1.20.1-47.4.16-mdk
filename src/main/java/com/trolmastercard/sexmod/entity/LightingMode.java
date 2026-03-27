package com.trolmastercard.sexmod.entity;

/**
 * LightingMode - Modos de Iluminación para Modelos Personalizados.
 * Portado a 1.20.1.
 * * Determina qué pipeline de iluminación se aplicará durante el renderizado
 * de la entidad o sus accesorios (outfits).
 */
public enum LightingMode {

    /** Utiliza la iluminación difusa estándar de las entidades en Minecraft. */
    DEFAULT,

    /** Utiliza el sombreador (Shader) personalizado de iluminación del mod. */
    SEXMOD,

    /** Brillo total (Fullbright). Ignora la iluminación del mundo (ideal para efectos brillantes). */
    NONE;
}