package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.OpenInteractionInventoryPacket;
import com.trolmastercard.sexmod.network.packet.SetNpcHomePacket;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;

/**
 * NpcInventoryEntity — Portado a 1.20.1.
 * * Maneja el inventario de 7 slots y la sincronización visual de ropa/armas.
 */
public abstract class NpcInventoryEntity extends BaseNpcEntity {

    // ── Data Accessors (Visuales) ─────────────────────────────────────────────
    public static final EntityDataAccessor<ItemStack> SLOT_MAIN_HAND = SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> SLOT_OFF_HAND  = SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> SLOT_EXTRA     = SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> SLOT_CHEST     = SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> SLOT_LEGS      = SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> SLOT_FEET      = SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<Integer>   MOOD_STATE     = SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.INT);

    // ── Alias de Compatibilidad (1.12.2 Legacy) ───────────────────────────────
    // Estos nombres permiten que tus clases de renderizado antiguas sigan compilando
    public static final EntityDataAccessor<ItemStack> SLOT_BOW   = SLOT_MAIN_HAND; // L
    public static final EntityDataAccessor<ItemStack> SLOT_SWORD = SLOT_OFF_HAND;  // R
    public static final EntityDataAccessor<ItemStack> SLOT_MISC  = SLOT_EXTRA;     // X
    public static final EntityDataAccessor<ItemStack> SLOT_BRA   = SLOT_CHEST;     // T
    public static final EntityDataAccessor<ItemStack> SLOT_LOWER = SLOT_LEGS;      // U
    public static final EntityDataAccessor<ItemStack> SLOT_SHOES = SLOT_FEET;      // W

    // ── Inventario Backend ────────────────────────────────────────────────────
    protected final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override
        protected void onContentsChanged(int slot) {
            if (slot < 6) { // Solo sincronizamos los primeros 6 slots visualmente
                syncInventoryToData(slot);
            }
        }
    };

    private final LazyOptional<IItemHandler> inventoryCap = LazyOptional.of(() -> inventory);

    protected NpcInventoryEntity(EntityType<? extends NpcInventoryEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLOT_MAIN_HAND, ItemStack.EMPTY);
        this.entityData.define(SLOT_OFF_HAND,  ItemStack.EMPTY);
        this.entityData.define(SLOT_EXTRA,     ItemStack.EMPTY);
        this.entityData.define(SLOT_CHEST,     ItemStack.EMPTY);
        this.entityData.define(SLOT_LEGS,      ItemStack.EMPTY);
        this.entityData.define(SLOT_FEET,      ItemStack.EMPTY);
        this.entityData.define(MOOD_STATE,     0);
    }

    private void syncInventoryToData(int slot) {
        EntityDataAccessor<ItemStack> accessor = switch (slot) {
            case 0 -> SLOT_MAIN_HAND;
            case 1 -> SLOT_OFF_HAND;
            case 2 -> SLOT_EXTRA;
            case 3 -> SLOT_CHEST;
            case 4 -> SLOT_LEGS;
            case 5 -> SLOT_FEET;
            default -> null;
        };
        if (accessor != null) {
            this.entityData.set(accessor, inventory.getStackInSlot(slot));
        }
    }
    /**
     * Hace que el NPC arroje el ítem que sostiene en la mano principal al suelo.
     * Se usa principalmente cuando a Luna no le gusta un regalo o en sus animaciones.
     */
    public void throwAwayCurrentItem() {
        if (this.level().isClientSide()) return;

        // Obtenemos el ítem de la mano principal (Slot 0)
        ItemStack stack = inventory.getStackInSlot(0);

        if (!stack.isEmpty()) {
            // 1. Crear la entidad física del ítem en el mundo
            double posX = this.getX();
            double posY = this.getY() + 1.5D; // Altura de las manos
            double posZ = this.getZ();

            net.minecraft.world.entity.item.ItemEntity itemEntity =
                    new net.minecraft.world.entity.item.ItemEntity(this.level(), posX, posY, posZ, stack.copy());

            // 2. Aplicar un pequeño impulso hacia adelante basado en la rotación del NPC
            float f = 0.3F;
            itemEntity.setDeltaMovement(
                    (double)(-net.minecraft.util.Mth.sin(this.getYRot() * 0.017453292F) * net.minecraft.util.Mth.cos(this.getXRot() * 0.017453292F) * f),
                    (double)(net.minecraft.util.Mth.sin(this.getXRot() * 0.017453292F) * f + 0.1F),
                    (double)(net.minecraft.util.Mth.cos(this.getYRot() * 0.017453292F) * net.minecraft.util.Mth.cos(this.getXRot() * 0.017453292F) * f)
            );

            itemEntity.setPickUpDelay(40); // Evita que se recoja instantáneamente
            this.level().addFreshEntity(itemEntity);

            // 3. Vaciar el slot del inventario (esto disparará syncInventoryToData automáticamente)
            inventory.setStackInSlot(0, ItemStack.EMPTY);

            // 4. Activar la animación visual
            this.setAnimStateFiltered(com.trolmastercard.sexmod.registry.AnimState.THROWN);
        }
    }
    // ── Lógica de Salud y IA ──────────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();

        // Curación automática si no hay combate (Cada 4 segundos / 80 ticks)
        if (this.tickCount % 80 == 0 && !this.level().isClientSide()) {
            if (this.getHealth() < this.getMaxHealth() && this.getTarget() == null) {
                // Verificar si hay enemigos (Monster) en un radio de 7 bloques
                AABB checkArea = this.getBoundingBox().inflate(7.0D);
                List<Monster> enemies = this.level().getEntitiesOfClass(Monster.class, checkArea);

                if (enemies.isEmpty()) {
                    this.heal(1.0F);
                    // Partículas de corazones en servidores
                    ((ServerLevel)this.level()).sendParticles(ParticleTypes.HEART,
                            getX(), getY() + 1.2, getZ(), 1, 0.2, 0.2, 0.2, 0.1);
                }
            }
        }
    }

    // ── Interacción (Acciones de la GUI) ──────────────────────────────────────

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "action.names.followme" -> setMaster(playerId.toString());
            case "action.names.stopfollowme" -> stopFollow();
            case "action.names.equipment" -> {
                if (this.level().isClientSide()) {
                    ModNetwork.CHANNEL.sendToServer(new OpenInteractionInventoryPacket(this.getUUID(), playerId));
                }
            }
            case "action.names.gohome" -> {
                stopFollow();
                // Aquí deberías tener una lógica para que el NPC camine a su HOME_POS
            }
            case "action.names.setnewhome" -> {
                ModNetwork.CHANNEL.sendToServer(new SetNpcHomePacket(this.getUUID(), this.blockPosition().getCenter()));
            }
        }
    }

    // ── Persistencia ──────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("NpcInventory", inventory.serializeNBT());
        tag.putInt("NpcMood", this.entityData.get(MOOD_STATE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcInventory")) {
            inventory.deserializeNBT(tag.getCompound("NpcInventory"));
            // Forzar actualización visual tras cargar el mundo
            for (int i = 0; i < 6; i++) syncInventoryToData(i);
        }
        this.entityData.set(MOOD_STATE, tag.getInt("NpcMood"));
    }

    // ── Soporte de Capabilities (Forge) ───────────────────────────────────────

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryCap.invalidate();
    }

    public ItemStackHandler getInventory() { return inventory; }
}
