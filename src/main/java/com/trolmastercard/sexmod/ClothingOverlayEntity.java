package com.trolmastercard.sexmod;

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
 * * Entidad invisible utilizada exclusivamente para renderizar ropa sobre NPCs/Jugadores
 * * usando el motor de animación de GeckoLib 4.
 */
public class ClothingOverlayEntity extends LivingEntity implements GeoEntity {

    static final float RENDER_DIST_SQ = 11_000f; // Aprox 104 bloques de distancia

    /** UUID del dueño (Sincronizado de forma nativa y comprimida) */
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    /** Etiqueta opcional del modelo de ropa */
    public static final EntityDataAccessor<String> MODEL_NAME =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public boolean displayOnly = false;
    @Nullable public ClothingSlotType slotType = null; // Asumiendo que es un Enum propio

    // ── Constructores ────────────────────────────────────────────────────────

    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level) {
        super(type, level);
    }

    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level, UUID ownerUuid, String modelName) {
        super(type, level);
        this.entityData.set(OWNER_UUID, Optional.of(ownerUuid));
        this.entityData.set(MODEL_NAME, modelName);
    }

    public static ClothingOverlayEntity createDisplay(EntityType<? extends ClothingOverlayEntity> type, Level level, UUID ownerUuid, ClothingSlotType slot) {
        ClothingOverlayEntity e = new ClothingOverlayEntity(type, level);
        e.entityData.set(OWNER_UUID, Optional.of(ownerUuid));
        e.displayOnly = true;
        e.slotType = slot;
        return e;
    }

    // ── Sincronización de Datos (Data Watcher) ───────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(MODEL_NAME, "");
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

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

    @Override
    public AABB getBoundingBoxForCulling() {
        // Expandimos la hitbox visual un poco más limpia para evitar que la ropa
        // desaparezca si el jugador mira el borde de la entidad.
        BlockPos bp = this.blockPosition();
        return new AABB(bp).inflate(2.0D);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < RENDER_DIST_SQ;
    }

    // ── Físicas y Daño (Entidad Fantasma) ────────────────────────────────────

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // En 1.20.1 se verifica usando el sistema de DamageTypes
        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD) && !source.is(DamageTypes.GENERIC_KILL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return false; }

    @Override protected void dropEquipment() {}

    // ── Equipamiento (Desactivado para ahorrar memoria) ──────────────────────

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList(); // Más rápido y ligero que instanciar un ArrayList
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
        // Al ser un overlay, las animaciones se suelen copiar del dueño en el Renderizador.
        // Si necesitas animaciones independientes de la ropa (ej. físicas de falda),
        // se registrarían aquí.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}