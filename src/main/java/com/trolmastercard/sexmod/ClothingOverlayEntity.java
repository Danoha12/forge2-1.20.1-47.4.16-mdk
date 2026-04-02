package com.trolmastercard.sexmod;

import com.trolmastercard.sexmod.registry.ClothingSlot; // 🚨 IMPORTANTE: El que forjamos antes
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
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
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * ClothingOverlayEntity — Portado a 1.20.1.
 * * Entidad invisible utilizada para renderizar ropa sobre NPCs/Jugadores.
 */
public class ClothingOverlayEntity extends LivingEntity implements GeoEntity {

    static final float RENDER_DIST_SQ = 11_000f; // Aprox 104 bloques

    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    public static final EntityDataAccessor<String> MODEL_NAME =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public boolean displayOnly = false;

    // 🚨 CORREGIDO: Usamos ClothingSlot en lugar de ClothingSlotType
    @Nullable public ClothingSlot slotType = null;

    // ── Constructores ────────────────────────────────────────────────────────

    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level) {
        super(type, level);
    }

    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level, UUID ownerUuid, String modelName) {
        super(type, level);
        this.entityData.set(OWNER_UUID, Optional.of(ownerUuid));
        this.entityData.set(MODEL_NAME, modelName);
    }

    // 🚨 CORREGIDO: Usamos ClothingSlot en los parámetros
    public static ClothingOverlayEntity createDisplay(EntityType<? extends ClothingOverlayEntity> type, Level level, UUID ownerUuid, ClothingSlot slot) {
        ClothingOverlayEntity e = new ClothingOverlayEntity(type, level);
        e.entityData.set(OWNER_UUID, Optional.of(ownerUuid));
        e.displayOnly = true;
        e.slotType = slot;
        return e;
    }

    // ── Sincronización de Datos ──────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(MODEL_NAME, "");
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    @Nullable
    public String getModelName() {
        String s = this.entityData.get(MODEL_NAME);
        return s.isEmpty() ? null : s;
    }

    // ── Renderizado y Culling ────────────────────────────────────────────────

    // 🚨 CORREGIDO: Quitamos los @Override conflictivos para los mapeos de Forge
    public AABB getBoundingBoxForCulling() {
        BlockPos bp = this.blockPosition();
        return new AABB(bp).inflate(2.0D);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < RENDER_DIST_SQ;
    }

    // ── Físicas y Daño ───────────────────────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override protected void dropEquipment() {}

    // ── Equipamiento ─────────────────────────────────────────────────────────

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.LEFT; }

    // ── GeckoLib ─────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}