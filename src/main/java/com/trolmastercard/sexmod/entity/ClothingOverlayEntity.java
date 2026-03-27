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
 * Entidad de Capa de Vestimenta - Portado a 1.20.1.
 * * Es una entidad invisible que se posiciona sobre un jugador o NPC para
 * renderizar capas adicionales de ropa o accesorios (Outfits).
 * * No posee colisiones físicas ni puede ser dañada, excepto por el vacío.
 */
public class ClothingOverlayEntity extends LivingEntity implements GeoEntity {

    // Distancia máxima a la que se renderizan las prendas (Optimizado)
    static final float RENDER_DIST_SQ = 11_000f;

    /** UUID del dueño (Jugador o NPC) al que pertenece esta prenda. */
    public static final EntityDataAccessor<String> OWNER_UUID =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.STRING);

    /** Nombre del modelo de la prenda (ej. "maid_dress", "nurse_outfit"). */
    public static final EntityDataAccessor<String> MODEL_NAME =
            SynchedEntityData.defineId(ClothingOverlayEntity.class, EntityDataSerializers.STRING);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Si es true, la entidad solo sirve para visualización en menús. */
    public boolean displayOnly = false;

    /** Define qué tipo de slot ocupa esta prenda (Pecho, Piernas, etc.). */
    @Nullable public ClothingSlotType slotType = null;

    // -- Constructores --

    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level) {
        super(type, level);
    }

    public ClothingOverlayEntity(EntityType<? extends ClothingOverlayEntity> type, Level level,
                                 UUID ownerUuid, String modelName) {
        this(type, level);
        this.entityData.set(OWNER_UUID, ownerUuid.toString());
        this.entityData.set(MODEL_NAME, modelName);
    }

    /** Factory para crear una previsualización de la prenda. */
    public static ClothingOverlayEntity createDisplay(
            EntityType<? extends ClothingOverlayEntity> type,
            Level level, UUID ownerUuid, ClothingSlotType slot) {
        ClothingOverlayEntity e = new ClothingOverlayEntity(type, level);
        e.getEntityData().set(OWNER_UUID, ownerUuid.toString());
        e.displayOnly = true;
        e.slotType = slot;
        return e;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OWNER_UUID, "");
        this.entityData.define(MODEL_NAME, "");
    }

    // -- Renderizado y Culling --

    @Override
    public AABB getBoundingBoxForCulling() {
        // Expandimos la caja de renderizado para evitar que la ropa desaparezca en los bordes de la pantalla
        BlockPos bp = blockPosition();
        return new AABB(bp.offset(-1, -1, -1), bp.offset(1, 1, 1));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < RENDER_DIST_SQ;
    }

    // -- Getters de Datos --

    @Nullable
    public UUID getOwnerUUID() {
        String s = this.entityData.get(OWNER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    @Nullable
    public String getModelName() {
        String s = this.entityData.get(MODEL_NAME);
        return s.isEmpty() ? null : s;
    }

    // -- Lógica de Daño e Inmunidad --

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // La ropa es invulnerable a todo excepto al daño de vacío (fuera del mundo)
        if (!source.is(level().damageSources().outOfWorld().typeHolder())) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override public boolean isPushable()  { return false; }
    @Override public boolean isPickable()  { return false; }
    @Override protected void dropEquipment() {}

    // -- Implementación de LivingEntity (Slots vacíos) --

    @Override
    public Iterable<ItemStack> getArmorSlots() { return new ArrayList<>(); }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    // -- GeckoLib 4 --

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // La ropa usualmente copia las animaciones del dueño, pero puede tener controladores propios aquí
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}