package com.trolmastercard.sexmod.inventory;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NpcEquipmentContainer (d4) — AbstractContainerMenu for the NPC clothing /
 * equipment screen.  Contains six NpcClothingSlots (WEAPON, BOW, HELMET,
 * CHEST_PLATE, PANTS, SHOES) plus the player's full inventory (36 slots).
 *
 * Tracks all open instances in the static list {@link #openContainers}.
 */
public class NpcEquipmentContainer extends AbstractContainerMenu {

    /** All currently open instances (used by packet handlers for sync). */
    public static final List<NpcEquipmentContainer> openContainers = new ArrayList<>();

    public final UUID ownerUuid;
    public final BaseNpcEntity npc;
    public final Slot[] clothingSlots;

    public NpcEquipmentContainer(MenuType<?> type, int windowId,
                                  BaseNpcEntity npc, Inventory playerInv, UUID ownerUuid) {
        super(type, windowId);
        this.ownerUuid = ownerUuid;
        this.npc = npc;
        openContainers.add(this);

        IItemHandler handler = npc.getCapability(
            net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, null)
            .orElseThrow(() -> new IllegalStateException("NPC missing item handler"));

        // --- Clothing slots (x positions match the 1.12.2 GUI layout) ---
        clothingSlots = new Slot[]{
            new NpcClothingSlot(ClothingSlotType.WEAPON,     handler, ClothingSlotType.WEAPON.id,     31,  60),
            new NpcClothingSlot(ClothingSlotType.BOW,        handler, ClothingSlotType.BOW.id,         50,  60),
            new NpcClothingSlot(ClothingSlotType.HELMET,     handler, ClothingSlotType.HELMET.id,      72,  60),
            new NpcClothingSlot(ClothingSlotType.CHEST_PLATE,handler, ClothingSlotType.CHEST_PLATE.id, 91,  60),
            new NpcClothingSlot(ClothingSlotType.PANTS,      handler, ClothingSlotType.PANTS.id,      110,  60),
            new NpcClothingSlot(ClothingSlotType.SHOES,      handler, ClothingSlotType.SHOES.id,      129,  60),
        };

        // --- Player inventory (rows 0-2) ---
        List<Slot> playerSlots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                playerSlots.add(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        // --- Player hotbar (row 3) ---
        for (int col = 0; col < 9; col++) {
            playerSlots.add(new Slot(playerInv, col, 8 + col * 18, 142));
        }

        for (Slot s : clothingSlots) addSlot(s);
        for (Slot s : playerSlots)   addSlot(s);
    }

    // ── Shift-click ───────────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            int equipSlots = slots.size() - player.getInventory().items.size();
            if (index < equipSlots) {
                if (!moveItemStackTo(stack, equipSlots, slots.size(), true))
                    return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, equipSlots, false))
                    return ItemStack.EMPTY;
            }
            if (stack.getCount() == 0) slot.set(ItemStack.EMPTY);
            else                       slot.setChanged();
            slot.onTake(player, stack);
        }
        return copy;
    }

    @Override
    public void setItem(int slot, int stateId, ItemStack stack) {
        super.setItem(slot, stateId, stack);
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void removed(Player player) {
        super.removed(player);
        openContainers.remove(this);
    }
}
