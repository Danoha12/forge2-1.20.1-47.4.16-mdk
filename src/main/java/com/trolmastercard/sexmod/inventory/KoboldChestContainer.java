package com.trolmastercard.sexmod.inventory;
import com.trolmastercard.sexmod.KoboldEntity;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * KoboldChestContainer - ported from bx.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A 27-slot (3-9) kobold chest inventory (the personal storage carried by a KoboldEntity)
 * plus the 36 standard player inventory slots.
 *
 * Layout:
 *   Slots 0-26  : Kobold chest (3 rows - 9 cols, Y offset 18-54)
 *   Slots 27-53 : Player main inventory (3 rows - 9, Y offset 85-121)
 *   Slots 54-62 : Player hotbar (1 row - 9, Y offset 143)
 *
 * Field mapping:
 *   a = playerInventory   (IInventory - Container - the player side)
 *   d = chestRows (3)
 *   b = static open-container list (List<bx> - List<KoboldChestContainer>)
 *   c = npcUUID (UUID)
 *
 * In 1.12.2:
 *   Container              - AbstractContainerMenu
 *   IInventory             - Container (net.minecraft.world.Container)
 *   func_174889_b(player)  - startOpen(player)
 *   func_174886_c(player)  - stopOpen(player)
 *   func_75146_a(slot)     - addSlot(slot)
 *   func_75135_a(...)      - moveItemStackTo(...)
 *   func_82846_b(...)      - quickMoveStack(...)
 *   ItemStack.field_190927_a - ItemStack.EMPTY
 *   func_190926_b()        - isEmpty()
 *   func_75211_c()         - getItem()
 *   func_75215_d(stack)    - set(stack)
 *   func_75218_e()         - setChanged()
 *   func_75216_d()         - hasItem()
 *
 * NOTE: MenuType is registered separately via DeferredRegister<MenuType<?>>.
 *       Pass {@code null} or your registered type as the first argument to
 *       {@link AbstractContainerMenu#AbstractContainerMenu(MenuType, int)}.
 */
public class KoboldChestContainer extends AbstractContainerMenu {

    /** Open containers list - used to notify all viewers of inventory changes. */
    public static List<KoboldChestContainer> openContainers = new ArrayList<>();

    private final Container playerContainer;
    private final int chestRows = 3;

    /** UUID of the kobold entity whose chest this belongs to. */
    public UUID npcUUID;

    /**
     * @param chestInventory  The kobold's 27-slot ItemStackHandler wrapped as Container.
     * @param playerInventory The player's inventory.
     * @param player          The player opening the container.
     * @param npcUUID         UUID of the owner kobold.
     */
    public KoboldChestContainer(Container chestInventory, Inventory playerInventory,
                                Player player, UUID npcUUID) {
        super(null /* register MenuType via DeferredRegister */, 0);
        this.npcUUID         = npcUUID;
        this.playerContainer = playerInventory;
        openContainers.add(this);

        chestInventory.startOpen(player);

        // ---- Kobold chest rows (3-9) ----------------------------------------
        for (int row = 0; row < chestRows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(chestInventory,
                    col + row * 9,
                    8 + col * 18,
                    18 + row * 18));
            }
        }

        // ---- Player main inventory (rows 1-3 of hotbar) ---------------------
        int yOffset = -18;   // original: byte b = -18
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory,
                    col + row * 9 + 9,
                    8 + col * 18,
                    103 + row * 18 + yOffset));
            }
        }

        // ---- Player hotbar --------------------------------------------------
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory,
                col,
                8 + col * 18,
                161 + yOffset));
        }
    }

    // =========================================================================
    //  AbstractContainerMenu overrides
    // =========================================================================

    @Override
    public boolean stillValid(Player player) {
        return playerContainer.stillValid(player);
    }

    /**
     * Shift-click handling. Items above the chest threshold shift to player
     * inventory; items in player inventory shift into the chest.
     * Original: {@code bx.func_82846_b(EntityPlayer, int)}
     */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            int chestEnd   = chestRows * 9;          // 27
            int totalSlots = this.slots.size();       // 27+36=63

            if (slotIndex < chestEnd) {
                // Chest - player inventory
                if (!this.moveItemStackTo(slotStack, chestEnd, totalSlots, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Player inventory - chest
                if (!this.moveItemStackTo(slotStack, 0, chestEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        playerContainer.stopOpen(player);
    }

    /** Returns the chest container (original: {@code bx.a()}). */
    public Container getChestInventory() {
        return playerContainer;
    }
}
