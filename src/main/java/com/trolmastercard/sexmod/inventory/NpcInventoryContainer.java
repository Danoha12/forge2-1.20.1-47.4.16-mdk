package com.trolmastercard.sexmod.inventory;
import com.trolmastercard.sexmod.NpcInventoryEntity;

import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.inventory.slot.ClothingSlotType;
import com.trolmastercard.sexmod.inventory.slot.NpcClothingSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NpcInventoryContainer - ported from ca.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A {@link AbstractContainerMenu} holding 7 clothing slots for an NPC entity
 * (one per {@link ClothingSlotType}) plus the player's 36 inventory slots.
 *
 * Slot layout:
 *   b[0] WEAPON       (41, 60)
 *   b[1] BOW          (59, 60)
 *   b[2] HELMET       (81, 60)
 *   b[3] CHEST_PLATE (100, 60)
 *   b[4] PANTS       (119, 60)
 *   b[5] SHOES       (138, 60)
 *   b[6] ROD          (22, 60)
 *   Slots 7-33  : Player main inventory (3-9, Y=84-120)
 *   Slots 34-42 : Player hotbar (1-9, Y=142)
 *
 * Field mapping:
 *   d = npcEntity (eb - NpcInventoryEntity)
 *   b = clothing slot array (Slot[7])
 *   a = npcUUID (UUID)
 *   c = static open-container list (List<ca> - List<NpcInventoryContainer>)
 *
 * In 1.12.2:
 *   CapabilityItemHandler.ITEM_HANDLER_CAPABILITY (EnumFacing.NORTH)
 *     - entity.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH)
 *   fe.b enum (ClothingSlot) - ClothingSlotType
 *   fe(ClothingSlotType, IItemHandler, id, x, y) - NpcClothingSlot(type, handler, id, x, y)
 *   func_75146_a(slot) - addSlot(slot)
 *   func_82846_b(player, index) - quickMoveStack(player, index)
 *   func_75135_a(stack, from, to, reverse) - moveItemStackTo(stack, from, to, reverse)
 *   field_75151_b.size() - slots.size()
 *   player.field_71071_by.field_70462_a.size() - player.getInventory().items.size()
 */
public class NpcInventoryContainer extends AbstractContainerMenu {

    /** All currently open NPC inventory containers. */
    public static List<NpcInventoryContainer> openContainers = new ArrayList<>();

    NpcInventoryEntity npcEntity;

    /** The 7 clothing slots (indexed by ClothingSlotType). */
    public Slot[] clothingSlots;

    /** UUID of the NPC whose inventory this represents. */
    public UUID npcUUID;

    /**
     * @param npc             The NPC entity whose clothing inventory to open.
     * @param playerInventory The opening player's inventory.
     * @param npcUUID         The NPC's UUID (used by client packets for targeting).
     */
    public NpcInventoryContainer(NpcInventoryEntity npc,
                                 Inventory playerInventory,
                                 UUID npcUUID) {
        super(null /* register MenuType via DeferredRegister */, 0);
        this.npcUUID    = npcUUID;
        openContainers.add(this);

        IItemHandler handler = npc.getItemHandler();
        if (handler == null) return;

        this.npcEntity = npc;

        // ---- 7 Clothing slots -----------------------------------------------
        this.clothingSlots = new Slot[] {
            new NpcClothingSlot(ClothingSlotType.WEAPON,      handler, ClothingSlotType.WEAPON.id,      41,  60),
            new NpcClothingSlot(ClothingSlotType.BOW,         handler, ClothingSlotType.BOW.id,         59,  60),
            new NpcClothingSlot(ClothingSlotType.HELMET,      handler, ClothingSlotType.HELMET.id,      81,  60),
            new NpcClothingSlot(ClothingSlotType.CHEST_PLATE, handler, ClothingSlotType.CHEST_PLATE.id, 100, 60),
            new NpcClothingSlot(ClothingSlotType.PANTS,       handler, ClothingSlotType.PANTS.id,       119, 60),
            new NpcClothingSlot(ClothingSlotType.SHOES,       handler, ClothingSlotType.SHOES.id,       138, 60),
            new NpcClothingSlot(ClothingSlotType.ROD,         handler, ClothingSlotType.ROD.id,         22,  60)
        };

        // ---- Player main inventory (3-9) ------------------------------------
        List<Slot> playerSlots = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                playerSlots.add(new Slot(playerInventory,
                    col + row * 9 + 9,
                    8 + col * 18,
                    84 + row * 18));
            }
        }

        // ---- Player hotbar --------------------------------------------------
        for (int col = 0; col < 9; col++) {
            playerSlots.add(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Register slots
        for (Slot s : clothingSlots) addSlot(s);
        for (Slot s : playerSlots)   addSlot(s);
    }

    // =========================================================================
    //  quickMoveStack  (shift-click)
    //  Original: ca.func_82846_b(EntityPlayer, int)
    // =========================================================================

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        // Clothing slots are at the start; player slots follow
        int npcSlotCount = this.slots.size() - player.getInventory().items.size();

        if (slotIndex < npcSlotCount) {
            // NPC slot - player inventory
            if (!this.moveItemStackTo(slotStack, npcSlotCount, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory - NPC slots
            if (!this.moveItemStackTo(slotStack, 0, npcSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.getCount() == 0) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        slot.onTake(player, slotStack);
        return result;
    }

    // =========================================================================
    //  Misc overrides
    // =========================================================================

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    /** Sync a single slot (original: {@code ca.func_75141_a(int, ItemStack)}). */
    @Override
    public void setItem(int slotId, int stateId, ItemStack stack) {
        super.setItem(slotId, stateId, stack);
    }
}
