package com.trolmastercard.sexmod.util;

import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.TextColor;

/**
 * KoboldColorVariant — Portado a 1.20.1.
 * * Define los 5 colores representativos de las tribus Kobold.
 * Se utiliza para:
 * - Colorear marcadores en el mapa.
 * - Resaltar elementos en la interfaz de usuario (UI).
 * - Definir el estilo de texto en los mensajes de la tribu.
 */
public enum KoboldColorVariant {

    LIGHT_GREEN (213, 239, 150),
    MEDIUM_GREEN(189, 165,  91),
    DARK_GREEN  (160, 183, 135),
    LIGHT_YELLOW(234, 176, 102),
    LIGHT_BLUE  (187, 203, 252);

    private final Vec3i rgb;
    private final TextColor textColor;

    KoboldColorVariant(int r, int g, int b) {
        this.rgb = new Vec3i(r, g, b);
        // Creamos el TextColor una sola vez para optimizar el renderizado de la UI
        this.textColor = TextColor.fromRgb((r << 16) | (g << 8) | b);
    }

    /**
     * Devuelve el color como un objeto Vec3i (R, G, B).
     */
    public Vec3i getRgb() {
        return this.rgb;
    }

    /**
     * Devuelve el color en formato TextColor (ideal para componentes de chat y UI).
     */
    public TextColor getTextColor() {
        return this.textColor;
    }

    /**
     * Devuelve el color en formato entero hexadecimal (0xRRGGBB).
     */
    public int getColorInt() {
        return this.textColor.getValue();
    }

    /**
     * Devuelve el índice (0-4) de la variante.
     * Mantenemos el nombre del método para compatibilidad con TribeUIValuesPacket.
     */
    public static int ordinalOf(KoboldColorVariant variant) {
        return variant.ordinal();
    }
}