package com.trolmastercard.sexmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.UUID;

/**
 * ClothingOverlayEntity (cy) - Invisible LivingEntity that renders a
 * clothing overlay on top of a player or NPC.
 *
 * DataParameters:
 *   OWNER_UUID  - the player/NPC UUID this overlay tracks
 *   MODEL_NAME  - optional model name string
 *
 * Only takes void/out-of-world damage; cannot be pushed or picked up.
 * Render distance: 11 000 blocks- (- - 104 blocks).
 */
public class ClothingOverlayEntity extends LivingEntity implements GeoEntity {

    static final float RENDER_DIST_SQ = 11_000f;

    /** Owner UUID (serialised as String; empty = no owner). */
    public static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.STRING);

    /** Optional model-name tag. */
    public static final EntityDataAccessor<String> MODEL_NAME =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** True when this entity is purely a display overlay (not physics-active). */
    public boolean displayOnly = false;

    /** The clothing slot type this overlay represents, if any. */
    @Nullable public String slotType = null;

    // -- Constructors ----------------------------------------------------------

    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level) {
        super(type, level);
    }

    /** Create a pure-display overlay for the given owner UUID. */
    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level,
                                 UUID ownerUuid, String modelName) {
        this(type, level);
        getEntityData().set(OWNER_UUID, ownerUuid.toString());
        getEntityData().set(MODEL_NAME, modelName);
    }

    /**
     * Factory: builds a display-only overlay bound to ownerUuid with the given
     * clothing slot.
     */
    public static ClothingOverlayEntity createDisplay(
            EntityType<? extends ClothingOverlayEntity> type,
            Level level, UUID ownerUuid, String slot) {
        ClothingOverlayEntity e = new ClothingOverlayEntity(type, level);
        e.getEntityData().set(OWNER_UUID, ownerUuid.toString());
        e.displayOnly = true;
        e.slotType = slot;
        return e;
    }

    // -- Data watcher ---------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_UUID, "");
        entityData.define(MODEL_NAME, "");
    }

    // -- Bounding box ---------------------------------------------------------

    @Override
    public AABB getBoundingBoxForCulling() {
        BlockPos bp = blockPosition();
        return new AABB(bp.offset(-1, -1, -1), bp.offset(1, 1, 1));
    }

    // -- Render visibility ----------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < RENDER_DIST_SQ;
    }

    // -- Owner access ---------------------------------------------------------

    @Nullable
    public UUID getOwnerUUID() {
        String s = entityData.get(OWNER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    @Nullable
    public String getModelName() {
        String s = entityData.get(MODEL_NAME);
        return s.isEmpty() ? null : s;
    }

    // -- Damage / physics -----------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only takes void damage
        if (!source.equals(level().damageSources().outOfWorld())) return false;
        return super.hurt(source, amount);
    }

    @Override public boolean isPushable()  { return false; }
    @Override public boolean isPickable()  { return false; }

    @Override
    protected void dropEquipment() {}

    // -- Equipment (no-op slots) -----------------------------------------------

    @Override
    public Iterable<ItemStack> getArmorSlots() { return new ArrayList<>(); }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.LEFT; }

    // -- GeckoLib -------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // -- Preview mode ---------------------------------------------------------

    /** True if this overlay is in preview/cross-display mode. */
    public boolean isPreviewMode() { return displayOnly; }

    /** Returns the texture/model name for this clothing overlay. */
    public String getTextureName() {
        String name = getModelName();
        return name != null ? name : "default";
    }

}