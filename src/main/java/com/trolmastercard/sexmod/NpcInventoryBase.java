package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcInventoryBase;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

/**
 * Abstract NPC entity that also implements a 27-slot inventory (Container).
 * Used as the base for NPCs that carry and display items in their inventory slot.
 *
 * The DATA_HAS_INVENTORY parameter (ID 111) was defined here in the original
 * hierarchy to avoid clashing with child-class IDs starting at 112.
 * In 1.20.1 we define it on the parent class token so the ID is stable.
 *
 * Obfuscated name: fo
 */
public abstract class NpcInventoryBase extends BaseNpcEntity implements Container {

    /**
     * Synced boolean - true once the NPC has had its inventory initialised.
     * Original ID: 111, defined on em.class in the obfuscated code.
     */
    public static final EntityDataAccessor<Boolean> DATA_HAS_INVENTORY =
            SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.BOOLEAN);

    /** The NPC's 27-slot inventory. */
    public final ItemStackHandler inventoryHandler = new ItemStackHandler(27);

    protected NpcInventoryBase(EntityType<? extends NpcInventoryBase> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HAS_INVENTORY, false);
    }

    // -- Container implementation -----------------------------------------------

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot >= this.inventoryHandler.getSlots()) return ItemStack.EMPTY;
        return this.inventoryHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return this.inventoryHandler.extractItem(slot, count, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.inventoryHandler.getStackInSlot(slot);
        return this.inventoryHandler.extractItem(slot, stack.getCount(), false);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventoryHandler.setStackInSlot(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public void setChanged() {}

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void startOpen(Player player) {}

    @Override
    public void stopOpen(Player player) {}

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public int getField(int id) {
        return id;
    }

    @Override
    public void setField(int id, int value) {}

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clearContent() {}
}
