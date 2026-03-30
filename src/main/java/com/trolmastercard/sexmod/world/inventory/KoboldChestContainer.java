package com.trolmastercard.sexmod.world.inventory;

import com.trolmastercard.sexmod.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * KoboldChestContainer — Portado a 1.20.1.
 * * Maneja el inventario de 27 slots de los Kobolds.
 * * Incluye lógica de Shift-Click (Quick Move) corregida para evitar loops.
 */
public class KoboldChestContainer extends AbstractContainerMenu {

    public static final List<KoboldChestContainer> openContainers = new ArrayList<>();

    private final Container koboldInventory;
    private final int chestRows = 3;
    public final UUID npcUUID;

    // Constructor para el Cliente (usado por el Screen)
    public KoboldChestContainer(int windowId, Inventory playerInv) {
        this(windowId, playerInv, new SimpleContainer(27), UUID.randomUUID());
    }

    // Constructor principal (Server/Common)
    public KoboldChestContainer(int windowId, Inventory playerInventory, Container koboldInventory, UUID npcUUID) {
        // Debes registrar ModMenus.KOBOLD_CHEST en tu DeferredRegister<MenuType<?>>
        super(ModMenus.KOBOLD_CHEST.get(), windowId);

        this.npcUUID = npcUUID;
        this.koboldInventory = koboldInventory;

        openContainers.add(this);
        koboldInventory.startOpen(playerInventory.player);

        // ---- Inventario del Kobold (3×9) ------------------------------------
        for (int row = 0; row < chestRows; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(koboldInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // ---- Inventario del Jugador (Principal) -----------------------------
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            }
        }

        // ---- Hotbar del Jugador ---------------------------------------------
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 143));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return this.koboldInventory.stillValid(player);
    }

    /**
     * Lógica de transferencia rápida (Shift-Click).
     * Crucial para que los ítems no desaparezcan al intentar moverlos.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 27) { // Del Kobold al Jugador
                if (!this.moveItemStackTo(itemstack1, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // Del Jugador al Kobold
                if (!this.moveItemStackTo(itemstack1, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.koboldInventory.stopOpen(player);
        openContainers.remove(this); // Importante: evita fugas de memoria
    }

    public Container getKoboldInventory() {
        return this.koboldInventory;
    }
}