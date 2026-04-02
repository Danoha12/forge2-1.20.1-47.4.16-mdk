package com.trolmastercard.sexmod.entity;

/**
 * LightingMode — Define cómo se renderiza la luz en el modelo.
 */
public enum LightingMode {
    DEFAULT,
    BRIGHT,
    DARK,
    EMISSIVE; // Para partes que brillan como ojos o runas

    public static LightingMode fromString(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return DEFAULT;
        }
    }
}