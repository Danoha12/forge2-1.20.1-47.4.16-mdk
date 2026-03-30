package com.trolmastercard.sexmod.client.renderer; // O el paquete donde tengas CustomModelManager

/**
 * LightingMode — Portado a 1.20.1.
 * * Enum usado por el CustomModelManager para definir cómo se ilumina un atuendo/outfit.
 */
public enum LightingMode {
    /** Usa la iluminación estándar y sombras de Minecraft. */
    DEFAULT,

    /** Usa el shader personalizado del mod (brillos/materiales especiales). */
    SEXMOD,

    /** Completamente brillante, ignora la oscuridad del mundo (estilo emisivo). */
    NONE;
}