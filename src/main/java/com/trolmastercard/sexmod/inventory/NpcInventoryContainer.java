package com.trolmastercard.sexmod.inventory; // Ajusta al paquete correcto

import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * NpcInventoryContainer — Portado a 1.20.1.
 * * Contenedor con 7 ranuras de ropa/armas (incluyendo la Caña de Pescar)
 * * más el inventario del jugador.
 */
public class NpcInventoryContainer extends AbstractContainerMenu {

    // 🚨 Lista segura para multihilos
    public static final List<NpcInventoryContainer> openContainers = new CopyOnWriteArrayList<>();

    public final NpcInventoryEntity npcEntity;
    public final UUID npcUUID;

    public NpcInventoryContainer(MenuType<?> type, int windowId, NpcInventoryEntity npc, Inventory playerInventory, UUID npcUUID) {
        super(type, windowId);
        this.npcUUID = npcUUID;
        this.npcEntity = npc;

        openContainers.add(this);

        IItemHandler handler = npc.getItemHandler();
        if (handler == null) return;

        // ── 7 Slots de Equipamiento del NPC ──
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.WEAPON, handler, ClothingSlotType.WEAPON.id, 41, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.BOW, handler, ClothingSlotType.BOW.id, 59, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.HELMET, handler, ClothingSlotType.HELMET.id, 81, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.CHEST_PLATE, handler, ClothingSlotType.CHEST_PLATE.id, 100, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.PANTS, handler, ClothingSlotType.PANTS.id, 119, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.SHOES, handler, ClothingSlotType.SHOES.id, 138, 60));
        this.addSlot(new NpcEquipmentSlot(ClothingSlotType.ROD, handler, ClothingSlotType.ROD.id, 22, 60));

        // ── Inventario principal del Jugador (3x9) ──
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // ── Hotbar del Jugador ──
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    // ── Lógica de Quick Move (Shift-Click) ───────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // 7 slots del NPC al principio
            int npcSlotCount = 7;

            if (slotIndex < npcSlotCount) {
                // Mover del NPC al jugador
                if (!this.moveItemStackTo(slotStack, npcSlotCount, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Mover del jugador al NPC
                if (!this.moveItemStackTo(slotStack, 0, npcSlotCount, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, slotStack);
        }
        return result;
    }

    // ── Seguridad y Limpieza ─────────────────────────────────────────────────

    @Override
    public boolean stillValid(Player player) {
        return this.npcEntity != null && this.npcEntity.isAlive() && player.distanceToSqr(this.npcEntity) < 64.0D;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // ¡CRÍTICO: Evita fugas de memoria en el servidor!
        openContainers.remove(this);
    }
}