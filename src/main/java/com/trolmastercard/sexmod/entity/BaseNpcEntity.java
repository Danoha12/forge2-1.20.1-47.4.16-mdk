package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.PathfinderMob;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BaseNpcEntity — Portado a 1.20.1.
 * * Implementa NpcStateAccessor para comunicación fluida con el renderizador.
 * * Maneja sincronización de datos, lógica de "Boy-Cam" y códigos de edición.
 */
public abstract class BaseNpcEntity extends PathfinderMob implements GeoEntity, NpcStateAccessor {

    // ── Registro Global ──────────────────────────────────────────────────────

    private static final Set<BaseNpcEntity> ALL_ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static Set<BaseNpcEntity> getAllActive() { return ALL_ACTIVE; }

    public static List<BaseNpcEntity> getByOwner(UUID ownerUUID) {
        List<BaseNpcEntity> result = new ArrayList<>();
        for (BaseNpcEntity npc : ALL_ACTIVE) {
            if (ownerUUID.equals(npc.getMasterUUID())) result.add(npc);
        }
        return result;
    }

    // ── DataParameters (Sincronización) ──────────────────────────────────────

    public static final EntityDataAccessor<String> MASTER_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> SHOULD_AT_TARGET = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TARGET_POS = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> NPC_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> MODEL_INDEX = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> ANIM_STATE = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> PARTNER_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);

    public Vec3 homePos = Vec3.ZERO;

    protected BaseNpcEntity(EntityType<? extends BaseNpcEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            ALL_ACTIVE.add(this);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NPC_UUID, UUID.randomUUID().toString());
        this.entityData.define(MODEL_INDEX, 0);
        this.entityData.define(ANIM_STATE, AnimState.NULL.name());
        this.entityData.define(PARTNER_UUID, "null");
        this.entityData.define(SHOULD_AT_TARGET, false);
        this.entityData.define(TARGET_YAW, 0.0F);
        this.entityData.define(TARGET_POS, "0|0|0");
        this.entityData.define(MASTER_UUID, "");
    }

    // ── Implementación de NpcStateAccessor (El Contrato) ─────────────────────

    @Override
    public UUID getSexPartnerUUID() {
        String s = this.entityData.get(PARTNER_UUID);
        return "null".equals(s) ? null : UUID.fromString(s);
    }

    @Override
    public void setSexPartnerUUID(@Nullable UUID partnerUUID) {
        this.entityData.set(PARTNER_UUID, partnerUUID == null ? "null" : partnerUUID.toString());
    }

    @Override
    public int getModelIndex() { return this.entityData.get(MODEL_INDEX); }

    @Override
    public void setModelIndex(int index) { this.entityData.set(MODEL_INDEX, index); }

    @Override
    public int getAnimationIndex() { return getAnimState().ordinal(); }

    @Override
    public void setAnimationIndex(int index) {
        AnimState[] states = AnimState.values();
        if (index >= 0 && index < states.length) setAnimStateFiltered(states[index]);
    }

    @Override
    public int getCumCounter() { return (int) getAnimState().ticksPlaying[0]; }

    @Override
    public void setCumCounter(int count) { getAnimState().ticksPlaying[0] = count; }

    @Override
    public void setAnimState(AnimState state) { this.setAnimStateFiltered(state); }

    @Override
    public AnimState getAnimState() {
        try { return AnimState.valueOf(this.entityData.get(ANIM_STATE)); }
        catch (Exception e) { return AnimState.NULL; }
    }

    // ── Lógica de Códigos (Wand Editor) ──────────────────────────────────────

    public String getModelCode() {
        return getModelIndex() + ":" + getCumCounter();
    }

    public static String getVariantCode(int variant) {
        return String.valueOf(variant);
    }

    public int getNpcVariant() { return 0; } // Sobrescribir en hijas

    // ── Suavizado de Cámara (Boy-Cam / Client side) ──────────────────────────

    @OnlyIn(Dist.CLIENT)
    public boolean hasSmoothPos() {
        return getAnimState() != null && getAnimState().useBoyCam;
    }

    @OnlyIn(Dist.CLIENT)
    public Vec3 getSmoothPos() {
        float pt = Minecraft.getInstance().getFrameTime();
        return new Vec3(
                Mth.lerp(pt, this.xo, this.getX()),
                Mth.lerp(pt, this.yo, this.getY()),
                Mth.lerp(pt, this.zo, this.getZ())
        );
    }

    // ── IA y Atributos ───────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 35.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new TemptGoal(this, 0.4, Ingredient.of(Items.BREAD, Items.WHEAT), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35));
    }

    // ── Ticks y Sincronización ───────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.entityData.get(SHOULD_AT_TARGET)) {
            Vec3 tp = getTargetPos();
            float yaw = this.entityData.get(TARGET_YAW);
            this.setYHeadRot(yaw);
            this.setYRot(yaw);
            this.moveTo(tp.x, tp.y, tp.z, yaw, this.getXRot());
        }
        if (this.homePos.equals(Vec3.ZERO)) this.homePos = this.position();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            AnimState state = getAnimState();
            if (state != AnimState.NULL) {
                state.ticksPlaying[0]++;
                if (state.ticksPlaying[0] >= state.length && state.followUp != null) {
                    this.setAnimStateFiltered(state.followUp);
                }
            }
        }
    }

    public void setAnimStateFiltered(AnimState next) {
        if (getAnimState() == next) return;
        this.entityData.set(ANIM_STATE, next.name());
        next.ticksPlaying[0] = 0;
        next.ticksPlaying[1] = 0;
    }

    // ── Persistencia (NBT) ───────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcUUID", this.entityData.get(NPC_UUID));
        tag.putString("MasterUUID", this.entityData.get(MASTER_UUID));
        tag.putInt("ModelIndex", getModelIndex());
        tag.putDouble("homeX", homePos.x); tag.putDouble("homeY", homePos.y); tag.putDouble("homeZ", homePos.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcUUID")) this.entityData.set(NPC_UUID, tag.getString("NpcUUID"));
        this.entityData.set(MASTER_UUID, tag.getString("MasterUUID"));
        this.setModelIndex(tag.getInt("ModelIndex"));
        this.homePos = new Vec3(tag.getDouble("homeX"), tag.getDouble("homeY"), tag.getDouble("homeZ"));
    }

    // ── Helpers Varios ───────────────────────────────────────────────────────

    public UUID getNpcUUID() { return UUID.fromString(this.entityData.get(NPC_UUID)); }

    @Nullable
    public UUID getMasterUUID() {
        String s = this.entityData.get(MASTER_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public void setMaster(String uuid) { this.entityData.set(MASTER_UUID, uuid); }

    public Vec3 getTargetPos() {
        String[] parts = this.entityData.get(TARGET_POS).split("\\|");
        return new Vec3(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
    }

    @Override
    public void remove(RemovalReason reason) {
        ALL_ACTIVE.remove(this);
        super.remove(reason);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) { return super.getDimensions(pose).scale(getModelScale()); }

    public float getModelScale() { return 1.0F; }

    public abstract Vec3 getBonePosition(String boneName);

    public abstract void triggerAction(String action, UUID playerId);
}