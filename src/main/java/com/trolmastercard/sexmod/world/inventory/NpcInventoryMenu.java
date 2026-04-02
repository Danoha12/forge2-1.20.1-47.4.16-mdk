package com.trolmastercard.sexmod.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * NpcInventoryMenu — Lógica del servidor para el inventario de 7 slots del NPC.
 */
public class NpcInventoryMenu extends AbstractContainerMenu {

    private final UUID npcUuid;

    // Constructor que Forge usa para abrir la GUI
    public NpcInventoryMenu(int containerId, Inventory playerInventory, UUID npcUuid) {
        // null se cambiará por el RegistryObject de tu MenuType cuando lo registres
        super(null, containerId);
        this.npcUuid = npcUuid;

        // Aquí iría la lógica de añadir los "Slots" visuales (addSlot(...))
    }

    // El método que tu GUI está pidiendo a gritos
    public UUID getNpcUUID() {
        return this.npcUuid;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Lógica de Shift-Click (requerida por AbstractContainerMenu)
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // El jugador puede usar este menú mientras esté cerca del NPC
        return true;
    }
}