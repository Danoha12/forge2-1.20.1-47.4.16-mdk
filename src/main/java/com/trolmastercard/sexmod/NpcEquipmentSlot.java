package com.trolmastercard.sexmod;

import net.minecraft.world.entity.EquipmentSlot;
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
 * Equipment slot that validates items by slot type.
 * Obfuscated name: fe
 */
public class NpcEquipmentSlot extends SlotItemHandler {

    private final SlotType slotType;

    public NpcEquipmentSlot(NpcEquipmentContainer container, IItemHandler handler,
                             int index, int x, int y) {
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
        Item item = stack.getItem();
        return switch (type) {
            case WEAPON     -> item instanceof SwordItem || item instanceof TieredItem;
            case BOW        -> item instanceof BowItem;
            case HELMET     -> item instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.HEAD;
            case CHEST_PLATE-> item instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.CHEST;
            case PANTS      -> item instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.LEGS;
            case SHOES      -> item instanceof ArmorItem a && a.getEquipmentSlot() == EquipmentSlot.FEET;
            case ROD        -> item instanceof FishingRodItem;
        };
    }

    // -- Slot type enum --------------------------------------------------------

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
                default -> throw new NullPointerException("Girls don't have a slot nr. " + index);
            };
        }
    }
}
