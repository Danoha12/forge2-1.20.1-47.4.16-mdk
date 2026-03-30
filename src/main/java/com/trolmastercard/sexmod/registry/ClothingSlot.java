package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.util.ModConstants;

/**
 * ClothingSlot — Identificadores de ranuras para la personalización de ropa.
 * * Vincula los elementos de la interfaz con los nombres de los huesos en el modelo .geo.json.
 */
public enum ClothingSlot {

    // IconXPos es la coordenada U en la textura clothing_icons.png (cada icono mide 20px)
    GIRL_SPECIFIC (160), // Slot especial para sliders (vibración, escala, etc.)
    HEAD           (0,   "customHead"),
    FOOT_L         (60,  "customShoeL"),
    FOOT_R         (80,  "customShoeR"),
    HAND_L         (100, "customHandL"),
    HAND_R         (120, "customHandR"),
    CUSTOM_BONE    (140, "customBone"); // Huesos extra añadidos dinámicamente

    public static final String SEPARATOR = "#";

    public final int iconXPos;
    public final String boneName;

    // Estos IDs se mantienen por compatibilidad con la lógica de red original,
    // aunque en 1.20.1 las interfaces modernas prefieren usar Listeners.
    public int buttonIDPlus;
    public int buttonIDMinus;

    /** Constructor para slots sin hueso fijo (como Girl Specific). */
    ClothingSlot(int iconXPos) {
        this.iconXPos = iconXPos;
        this.boneName = null;
    }

    /** Constructor principal con mapeo de huesos de GeckoLib. */
    ClothingSlot(int iconXPos, String boneName) {
        this.iconXPos = iconXPos;
        this.boneName = boneName;

        // Inicializamos los IDs de botones usando un contador global si es necesario
        // Asegúrate de tener public static int BUTTON_ID_COUNTER = 0; en ModConstants
        this.buttonIDPlus = ++ModConstants.BUTTON_ID_COUNTER;
        this.buttonIDMinus = ++ModConstants.BUTTON_ID_COUNTER;
    }

    /** * Devuelve la cantidad de slots que están mapeados directamente a huesos
     * (Excluye GIRL_SPECIFIC y CUSTOM_BONE de la cuenta estándar).
     */
    public static int boneMappedCount() {
        return values().length - 2;
    }

    /** * Busca un slot por el nombre del hueso.
     */
    public static ClothingSlot fromBoneName(String name) {
        for (ClothingSlot slot : values()) {
            if (slot.boneName != null && slot.boneName.equals(name)) {
                return slot;
            }
        }
        return null;
    }
}