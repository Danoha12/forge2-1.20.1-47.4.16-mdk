package com.trolmastercard.sexmod.entity; // Ajusta a tu paquete de entidades

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * NpcInventoryBase — Portado a 1.20.1.
 * * Entidad NPC abstracta que implementa un inventario de 27 espacios.
 * * A prueba de reinicios (guarda en NBT) y expuesto a Forge vía Capabilities.
 */
public abstract class NpcInventoryBase extends BaseNpcEntity implements Container {

    public static final EntityDataAccessor<Boolean> DATA_HAS_INVENTORY =
            SynchedEntityData.defineId(NpcInventoryBase.class, EntityDataSerializers.BOOLEAN);

    // ── Sistema de Inventario de Forge ───────────────────────────────────────

    public final ItemStackHandler inventoryHandler = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged(); // Avisa a la entidad cuando un ítem cambia
        }
    };

    // LazyOptional es obligatorio en Forge 1.20+ para devolver Capabilities
    private final LazyOptional<IItemHandler> inventoryOptional = LazyOptional.of(() -> inventoryHandler);

    protected NpcInventoryBase(EntityType<? extends NpcInventoryBase> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HAS_INVENTORY, false);
    }

    // ── Guardado y Carga de Memoria (NBT) VITAL ──────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        // Guardamos los ítems
        compound.put("NpcInventory", this.inventoryHandler.serializeNBT());
        compound.putBoolean("HasInventory", this.entityData.get(DATA_HAS_INVENTORY));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        // Cargamos los ítems al entrar al mundo
        if (compound.contains("NpcInventory")) {
            this.inventoryHandler.deserializeNBT(compound.getCompound("NpcInventory"));
        }
        this.entityData.set(DATA_HAS_INVENTORY, compound.getBoolean("HasInventory"));
    }

    // ── Exposición a Forge (Capabilities) ────────────────────────────────────

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryOptional.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryOptional.invalidate(); // Limpieza de memoria al destruir la entidad
    }

    // ── Implementación de Container (Vanilla Fallback) ───────────────────────

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.inventoryHandler.getSlots(); i++) {
            if (!this.inventoryHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.inventoryHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.inventoryHandler.extractItem(slot, count, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.inventoryHandler.getStackInSlot(slot);
        this.inventoryHandler.extractItem(slot, stack.getCount(), false);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventoryHandler.setStackInSlot(slot, stack);
    }

    @Override
    public void setChanged() {
        // En 1.20.1, no es necesario hacer mucho aquí a menos que quieras actualizar bloques
    }

    @Override
    public boolean stillValid(Player player) {
        // Seguridad contra jugadores alejados
        return this.isAlive() && player.distanceToSqr(this) < 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < this.inventoryHandler.getSlots(); i++) {
            this.inventoryHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }
}