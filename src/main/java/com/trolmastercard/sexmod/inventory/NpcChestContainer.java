package com.trolmastercard.sexmod.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NpcChestContainer — Esqueleto para que compile la GUI.
 * * Nota: En la 1.20.1 deberás registrar este menú en tu clase principal (DeferredRegister<MenuType<?>>).
 */
public class NpcChestContainer extends AbstractContainerMenu {

    // La lista estática que tu NpcInventoryGuiScreen estaba buscando
    public static final List<NpcChestContainer> openContainers = new ArrayList<>();

    private final UUID npcUuid;

    public NpcChestContainer(int id, UUID npcUuid) {
        super(null, id); // "null" debe reemplazarse por tu ModMenuTypes.NPC_CHEST.get() cuando lo registres
        this.npcUuid = npcUuid;
        openContainers.add(this);
    }

    public UUID getNpcUUID() {
        return this.npcUuid;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Lógica de Shift-Click (Por ahora vacío para que compile)
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        // Verifica si el jugador está lo suficientemente cerca del NPC
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        openContainers.remove(this); // Nos limpiamos de la lista al cerrar
    }
}