package com.trolmastercard.sexmod.entity;

import net.minecraft.core.Vec3i;

/**
 * KoboldColorVariant - Variantes de Color para la Tribu.
 * Portado a 1.20.1.
 * * Define los cinco colores oficiales usados para el resaltado de la interfaz (UI)
 * y la coloración de marcadores en el mapa. Cada variante almacena un valor RGB.
 */
public enum KoboldColorVariant {

    LIGHT_GREEN (213, 239, 150),
    MEDIUM_GREEN(189, 165,  91),
    DARK_GREEN  (160, 183, 135),
    LIGHT_YELLOW(234, 176, 102),
    LIGHT_BLUE  (187, 203, 252);

    /** Color RGB almacenado como un vector de enteros. */
    private final Vec3i rgb;

    KoboldColorVariant(int r, int g, int b) {
        this.rgb = new Vec3i(r, g, b);
    }

    /** * Retorna el triplete RGB de la variante.
     */
    public Vec3i getRgb() {
        return rgb;
    }

    /**
     * Retorna el índice (ordinal) de la variante proporcionada.
     * Usado por el sistema de red para sincronizar colores de la UI.
     * * @param variant La variante de color a buscar.
     * @return El índice 0-4 de la variante, o el tamaño del enum si no se encuentra.
     */
    public static int ordinalOf(KoboldColorVariant variant) {
        int i = 0;
        for (KoboldColorVariant v : values()) {
            if (variant == v) return i;
            i++;
        }
        return i;
    }
}