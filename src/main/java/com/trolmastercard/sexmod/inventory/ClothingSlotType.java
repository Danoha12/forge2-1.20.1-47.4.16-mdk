package com.trolmastercard.sexmod.inventory;

/**
 * ClothingSlotType — Define los índices y tipos de equipamiento para los NPCs.
 * Portado a 1.20.1.
 */
public enum ClothingSlotType {
    WEAPON(0),
    BOW(1),
    HELMET(2),
    CHEST_PLATE(3),
    PANTS(4),
    SHOES(5);

    public final int id;

    ClothingSlotType(int id) {
        this.id = id;
    }

    /**
     * Obtiene el tipo basado en el ID del slot.
     */
    public static ClothingSlotType fromId(int id) {
        for (ClothingSlotType type : values()) {
            if (type.id == id) return type;
        }
        return WEAPON; // Valor por defecto
    }
}