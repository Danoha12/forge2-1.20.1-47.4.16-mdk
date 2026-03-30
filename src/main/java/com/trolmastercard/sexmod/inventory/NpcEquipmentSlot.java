package com.trolmastercard.sexmod.inventory; // Ajusta al paquete correcto

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * NpcEquipmentSlot — Portado a 1.20.1.
 * * Slot especializado que valida los ítems antes de permitir que el jugador
 * * los coloque en el inventario del NPC.
 */
public class NpcEquipmentSlot extends SlotItemHandler {

    private final SlotType slotType;

    public NpcEquipmentSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.slotType = SlotType.fromIndex(index);
    }

    public static boolean isValidForSlot(ItemStack stack, int slotIndex) {
        return isValidForSlot(stack, SlotType.fromIndex(slotIndex));
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isValidForSlot(stack, this.slotType);
    }

    static boolean isValidForSlot(ItemStack stack, SlotType type) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();

        return switch (type) {
            case WEAPON      -> item instanceof SwordItem || item instanceof TieredItem;
            case BOW         -> item instanceof BowItem;
            // 🚨 1.20.1: Mojang usa ArmorItem.Type ahora en lugar de EquipmentSlot
            case HELMET      -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.HELMET;
            case CHEST_PLATE -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.CHESTPLATE;
            case PANTS       -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.LEGGINGS;
            case SHOES       -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.BOOTS;
            case ROD         -> item instanceof FishingRodItem;
        };
    }

    // ── Enum Interno ─────────────────────────────────────────────────────────

    public enum SlotType {
        WEAPON(0),
        BOW(1),
        HELMET(2),
        CHEST_PLATE(3),
        PANTS(4),
        SHOES(5),
        ROD(6);

        public final int id;

        SlotType(int id) {
            this.id = id;
        }

        public static SlotType fromIndex(int index) {
            return switch (index) {
                case 0 -> WEAPON;
                case 1 -> BOW;
                case 2 -> HELMET;
                case 3 -> CHEST_PLATE;
                case 4 -> PANTS;
                case 5 -> SHOES;
                case 6 -> ROD;
                // IllegalArgumentException es el estándar de Java para índices fuera de rango
                default -> throw new IllegalArgumentException("Girls don't have a slot nr. " + index);
            };
        }
    }
}