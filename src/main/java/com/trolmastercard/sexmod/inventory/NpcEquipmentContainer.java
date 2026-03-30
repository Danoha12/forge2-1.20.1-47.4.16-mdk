package com.trolmastercard.sexmod.inventory; // Ajusta a tu paquete

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NpcEquipmentContainer — Portado a 1.20.1.
 * * Contenedor del servidor para equipar armas y armaduras al NPC.
 */
public class NpcEquipmentContainer extends AbstractContainerMenu {

    // 🚨 1.20.1: Usamos CopyOnWriteArrayList para evitar ConcurrentModificationException
    // cuando varios jugadores abren/cierran menús al mismo tiempo en el servidor.
    public static final List<NpcEquipmentContainer> openContainers = new CopyOnWriteArrayList<>();

    public final UUID ownerUuid;
    public final BaseNpcEntity npc;

    public NpcEquipmentContainer(MenuType<?> type, int windowId, BaseNpcEntity npc, Inventory playerInv, UUID ownerUuid) {
        super(type, windowId);
        this.ownerUuid = ownerUuid;
        this.npc = npc;

        openContainers.add(this);

        // Obtenemos el inventario del NPC a través de Capabilities
        IItemHandler handler = npc.getCapability(ForgeCapabilities.ITEM_HANDLER, null)
                .orElseThrow(() -> new IllegalStateException("El NPC no tiene la Capability ITEM_HANDLER asignada."));

        // ── Slots de Equipamiento del NPC (Índices 0 - 5) ──
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.WEAPON, handler, ClothingSlotType.WEAPON.id, 31, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.BOW, handler, ClothingSlotType.BOW.id, 50, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.HELMET, handler, ClothingSlotType.HELMET.id, 72, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.CHEST_PLATE, handler, ClothingSlotType.CHEST_PLATE.id, 91, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.PANTS, handler, ClothingSlotType.PANTS.id, 110, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.SHOES, handler, ClothingSlotType.SHOES.id, 129, 60));

        // ── Inventario del Jugador (Índices 6 - 32) ──
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // ── Hotbar del Jugador (Índices 33 - 41) ──
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    // ── Lógica de Shift-Click (Quick Move) ───────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();

            // Los primeros 6 slots son del NPC
            int equipSlots = 6;

            if (index < equipSlots) {
                // Mover del NPC al inventario del jugador
                if (!this.moveItemStackTo(stack, equipSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Mover del inventario del jugador al NPC
                if (!this.moveItemStackTo(stack, 0, equipSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        // Validación básica: el NPC debe seguir vivo y el jugador cerca
        return this.npc != null && this.npc.isAlive() && player.distanceToSqr(this.npc) < 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        openContainers.remove(this);
    }
}