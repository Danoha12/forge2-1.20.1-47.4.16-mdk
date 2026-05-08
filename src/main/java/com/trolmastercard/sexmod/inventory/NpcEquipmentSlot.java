package com.trolmastercard.sexmod.inventory;

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
 * * Slot especializado que valida los ítems antes de permitir el equipamiento.
 */
public class NpcEquipmentSlot extends SlotItemHandler {

    private final ClothingSlotType slotType;

    // ── CONSTRUCTOR SINCRONIZADO (5 Parámetros) ──────────────────────────────
    public NpcEquipmentSlot(ClothingSlotType type, IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.slotType = type;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isValidForSlot(stack, this.slotType);
    }

    public static boolean isValidForSlot(ItemStack stack, ClothingSlotType type) {
        if (stack.isEmpty()) return false;

        Item item = stack.getItem();

        return switch (type) {
            case WEAPON      -> item instanceof SwordItem || item instanceof TieredItem;
            case BOW         -> item instanceof BowItem;
            // 🚨 1.20.1: Usamos ArmorItem.Type para la validación exacta
            case HELMET      -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.HELMET;
            case CHEST_PLATE -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.CHESTPLATE;
            case PANTS       -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.LEGGINGS;
            case SHOES       -> item instanceof ArmorItem a && a.getType() == ArmorItem.Type.BOOTS;
            case ROD         -> item instanceof FishingRodItem;
        };
    }
}