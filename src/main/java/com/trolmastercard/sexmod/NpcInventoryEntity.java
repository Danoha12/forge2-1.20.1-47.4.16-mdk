package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;

/**
 * NpcInventoryEntity - ported from e2.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Abstract base class for NPC entities that carry an inventory and sync 6 equipment
 * slot {@link ItemStack}s via {@link SynchedEntityData}.
 *
 * Equipment DataParameters (IDs 112-117):
 *   L  (117) - slot 0 (e.g. bow)
 *   R  (116) - slot 1 (e.g. sword)
 *   X  (115) - slot 2
 *   T  (114) - slot 3 (bra / chest clothing)
 *   U  (113) - slot 4 (lower clothing)
 *   W  (112) - slot 5 (shoes/socks)
 *   M  (111) - mood/state integer
 *
 * Inventory has 7 slots; the first two are pre-filled with bow + sword defaults.
 *
 * Healing loop (every 80 ticks): heals 1 HP if no nearby mobs and not at max health.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - EntityDataManager.func_187226_a - SynchedEntityData.defineId
 *   - DataSerializers.field_187196_f - EntityDataSerializers.ITEM_STACK
 *   - DataSerializers.field_187192_b - EntityDataSerializers.INT
 *   - func_70088_a() - defineSynchedData()
 *   - func_184651_r() - registerGoals(); g - goal (NpcBreedGoal)
 *   - func_70619_bc() - aiStep()
 *   - func_110143_aJ() - getHealth(); func_110138_aP() - getMaxHealth()
 *   - func_70691_i(1.0F) - heal(1.0F)
 *   - J() - isInCombat() / method on base entity
 *   - EntityMob.class - Monster.class
 *   - new AxisAlignedBB(BlockPos, BlockPos) - new AABB(x1,y1,z1,x2,y2,z2)
 *   - WorldServer.func_180505_a(EnumParticleTypes.HEART, ...) - level.addParticle(ParticleTypes.HEART,...)
 *   - ItemStack.field_190927_a - ItemStack.EMPTY
 *   - Items.field_151040_l - Items.BOW; Items.field_151031_f - Items.IRON_SWORD
 *   - this.m.func_187227_b - entityData.set
 *   - field_184621_as - EntityDataAccessors (air/etc, set byte "1")
 *   - CapabilityItemHandler.ITEM_HANDLER_CAPABILITY - ForgeCapabilities.ITEM_HANDLER
 *   - ge.b.sendToServer - ModNetwork.CHANNEL.sendToServer
 *   - bo - OpenNpcInventoryPacket; gg - SetNpcHomePacket; a6 - SetNpcHomePacket with pos
 *   - a("action.names.followme", ...) - setMaster(uuid)
 *   - x() - stopFollow()
 *   - c() - cancelCurrentAction()
 *   - NBTTagCompound.func_74782_a - tag.put; func_74775_l - tag.getCompound
 *   - super.func_70014_b / func_70037_a - addAdditionalSaveData / readAdditionalSaveData
 *   - EnumFacing - Direction
 */
public abstract class NpcInventoryEntity extends BaseNpcEntity {

    // -- Inventory --------------------------------------------------------------
    public final ItemStackHandler inventory = new ItemStackHandler(7);

    // -- Synced equipment DataParameters ----------------------------------------
    public static final EntityDataAccessor<ItemStack> SLOT_BOW   =
            SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> SLOT_SWORD =
            SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    public static final EntityDataAccessor<ItemStack> SLOT_2     =
            SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Bra / chest clothing slot. */
    public static final EntityDataAccessor<ItemStack> SLOT_BRA   =
            SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Lower clothing / slip slot. */
    public static final EntityDataAccessor<ItemStack> SLOT_LOWER =
            SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Shoes / socks slot. */
    public static final EntityDataAccessor<ItemStack> SLOT_SHOES =
            SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.ITEM_STACK);
    /** Mood / state integer. */
    public static final EntityDataAccessor<Integer>   MOOD       =
            SynchedEntityData.defineId(NpcInventoryEntity.class, EntityDataSerializers.INT);

    // -- Canonical aliases matching original e2 field names (L/R/X/T/U/W) ------
    // These are used by GoblinBodyRenderer, NpcInventoryRenderer, and packet classes.
    /** e2.L (id 117) - bow slot. */
    public static final EntityDataAccessor<ItemStack> L          = SLOT_BOW;
    /** e2.R (id 116) - sword slot. */
    public static final EntityDataAccessor<ItemStack> R          = SLOT_SWORD;
    /** e2.X (id 115) - slot 2. */
    public static final EntityDataAccessor<ItemStack> X          = SLOT_2;
    /** e2.T (id 114) - bra/chest clothing slot. Also aliased as CHEST_ITEM. */
    public static final EntityDataAccessor<ItemStack> T          = SLOT_BRA;
    /** e2.U (id 113) - lower clothing/slip slot. Also aliased as LEG_ITEM. */
    public static final EntityDataAccessor<ItemStack> U          = SLOT_LOWER;
    /** e2.W (id 112) - shoes/socks slot. Also aliased as FEET_ITEM. */
    public static final EntityDataAccessor<ItemStack> W          = SLOT_SHOES;

    // Descriptive aliases used by GoblinBodyRenderer bone-tint logic
    public static final EntityDataAccessor<ItemStack> CHEST_ITEM = SLOT_BRA;
    public static final EntityDataAccessor<ItemStack> LEG_ITEM   = SLOT_LOWER;
    public static final EntityDataAccessor<ItemStack> FEET_ITEM  = SLOT_SHOES;

    /** Returns an array of the 6 equipment slot accessors in original order (L,R,X,T,U,W). */
    public static EntityDataAccessor<?>[] getEquipmentSlotKeys(NpcInventoryEntity entity) {
        return new EntityDataAccessor<?>[]{ L, R, X, T, U, W };
    }

    /** Convenience: returns the inventory handler. */
    public ItemStackHandler getInventory() { return inventory; }

    // -- Other state ------------------------------------------------------------
    public int    mood      = 1;
    public int    xpValue   = 0;
    public int    healTimer = 0;
    public boolean isN      = false;

    // -- Constructor ------------------------------------------------------------

    protected NpcInventoryEntity(net.minecraft.world.entity.EntityType<? extends NpcInventoryEntity> type, Level level) {
        super(type, level);
        // Default inventory
        if (inventory.getStackInSlot(0).isEmpty())
            inventory.setStackInSlot(0, new ItemStack(Items.BOW));
        if (inventory.getStackInSlot(1).isEmpty())
            inventory.setStackInSlot(1, new ItemStack(Items.IRON_SWORD));
    }

    // -- Synced data ------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MOOD,       0);
        this.entityData.define(SLOT_BOW,   ItemStack.EMPTY);
        this.entityData.define(SLOT_SWORD, ItemStack.EMPTY);
        this.entityData.define(SLOT_2,     ItemStack.EMPTY);
        this.entityData.define(SLOT_BRA,   ItemStack.EMPTY);
        this.entityData.define(SLOT_LOWER, ItemStack.EMPTY);
        this.entityData.define(SLOT_SHOES, ItemStack.EMPTY);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new NpcBreedGoal(this));
    }

    // -- AI step ----------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();

        // Sync equipment every tick (cheap - only sends if dirty)
        this.entityData.set(SLOT_BOW,   inventory.getStackInSlot(0));
        this.entityData.set(SLOT_SWORD, inventory.getStackInSlot(1));
        this.entityData.set(SLOT_2,     inventory.getStackInSlot(2));
        this.entityData.set(SLOT_BRA,   inventory.getStackInSlot(3));
        this.entityData.set(SLOT_LOWER, inventory.getStackInSlot(4));
        this.entityData.set(SLOT_SHOES, inventory.getStackInSlot(5));

        // Healing tick (every 80 ticks)
        if (this.tickCount % 80 == 0) {
            if (getHealth() != getMaxHealth() && !isInCombat()) {
                List<Monster> mobs = level.getEntitiesOfClass(Monster.class,
                        new AABB(getX() - 7, getY() - 1, getZ() - 7,
                                 getX() + 7, getY() + 1, getZ() + 7));
                boolean safe = mobs.isEmpty();
                heal(safe ? 1.0F : 0.0F);
                if (safe && level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.HEART,
                            getX(), getY() + 1.0 + random.nextDouble(), getZ(),
                            1, random.nextGaussian(), random.nextGaussian(), random.nextGaussian(), 0.0);
                }
            }
        }

        // Clear combat flag when safe
        if (isN && !isInCombat()) isN = false;
    }

    // -- Client-side action handler ---------------------------------------------

    @Override
    public void triggerAction(String action, UUID playerId) {
        switch (action) {
            case "action.names.followme" ->
                setMaster(playerId.toString());
            case "action.names.stopfollowme" ->
                stopFollow();
            case "action.names.equipment" -> {
                net.minecraft.client.player.LocalPlayer sp =
                        net.minecraft.client.Minecraft.getInstance().player;
                if (sp != null)
                    ModNetwork.CHANNEL.sendToServer(
                            new OpenNpcInventoryPacket(getNpcUUID(), sp.getGameProfile().getId()));
            }
            case "action.names.gohome" -> {
                stopFollow();
                ModNetwork.CHANNEL.sendToServer(new SetNpcHomePacket(getNpcUUID()));
            }
            case "action.names.setnewhome" -> {
                cancelCurrentAction();
                ModNetwork.CHANNEL.sendToServer(
                        new SetNpcHomePacket(getNpcUUID(), blockPosition()));
            }
        }
    }

    // -- NBT --------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.put("inventory", inventory.serializeNBT());
        super.addAdditionalSaveData(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        inventory.deserializeNBT(tag.getCompound("inventory"));
    }

    // -- Capability (Forge item handler) ----------------------------------------

    private final LazyOptional<ItemStackHandler> invCap =
            LazyOptional.of(() -> inventory);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction face) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return invCap.cast();
        return super.getCapability(cap, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invCap.invalidate();
    }
}
