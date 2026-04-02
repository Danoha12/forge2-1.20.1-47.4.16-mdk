package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.SexAnimationTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity; // Importación necesaria para RemovalReason
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.PathfinderMob; // CAMBIADO: Antes estaba en .monster
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.animatable.GeoEntity;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseNpcEntity extends PathfinderMob implements GeoEntity, NpcStateAccessor {

    private static final Set<BaseNpcEntity> ALL_ACTIVE = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static Set<BaseNpcEntity> getAllActive() { return ALL_ACTIVE; }

    // DataParameters
    public static final EntityDataAccessor<String> MASTER_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> SHOULD_AT_TARGET = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TARGET_POS = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> NPC_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> MODEL_INDEX = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> ANIM_STATE = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String> PARTNER_UUID = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer> ANIM_TICK = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> INTERACTION_LEVEL = SynchedEntityData.defineId(BaseNpcEntity.class, EntityDataSerializers.FLOAT);

    public Vec3 homePos = Vec3.ZERO;

    protected BaseNpcEntity(EntityType<? extends BaseNpcEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            ALL_ACTIVE.add(this);
        }
    }

    public static List<BaseNpcEntity> getAllWithMaster(UUID masterUUID) {
        return List.of();
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
        this.entityData.define(INTERACTION_LEVEL, 0.0F);
        this.entityData.define(ANIM_TICK, 0);
    }

    public float getInteractionLevel() { return this.entityData.get(INTERACTION_LEVEL); }
    public void setInteractionLevel(float level) { this.entityData.set(INTERACTION_LEVEL, Mth.clamp(level, 0.0F, 1.0F)); }
    public int getAnimTick() { return this.entityData.get(ANIM_TICK); }
    public void setAnimTick(int tick) { this.entityData.set(ANIM_TICK, tick); }

    @Override
    public UUID getSexPartnerUUID() {
        String s = this.entityData.get(PARTNER_UUID);
        return "null".equals(s) ? null : UUID.fromString(s);
    }

    @Override
    public void setSexPartnerUUID(@Nullable UUID partnerUUID) {
        this.entityData.set(PARTNER_UUID, partnerUUID == null ? "null" : partnerUUID.toString());
    }

    @Override public int getModelIndex() { return this.entityData.get(MODEL_INDEX); }
    @Override public void setModelIndex(int index) { this.entityData.set(MODEL_INDEX, index); }
    @Override public int getAnimationIndex() { return getAnimState().ordinal(); }

    @Override
    public void setAnimationIndex(int index) {
        AnimState[] states = AnimState.values();
        if (index >= 0 && index < states.length) setAnimStateFiltered(states[index]);
    }

    @Override public int getCumCounter() { return getAnimTick(); }
    @Override public void setCumCounter(int count) { setAnimTick(count); }
    @Override public void setAnimState(AnimState state) { this.setAnimStateFiltered(state); }

    @Override
    public AnimState getAnimState() {
        try { return AnimState.valueOf(this.entityData.get(ANIM_STATE)); }
        catch (Exception e) { return AnimState.NULL; }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean hasSmoothPos() { return getAnimState() != null && getAnimState().useBoyCam; }

    @OnlyIn(Dist.CLIENT)
    public Vec3 getSmoothPos() {
        float pt = Minecraft.getInstance().getFrameTime();
        return new Vec3(Mth.lerp(pt, this.xo, this.getX()), Mth.lerp(pt, this.yo, this.getY()), Mth.lerp(pt, this.zo, this.getZ()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 35.0);
    }

    public void stopFollow() {
        setMaster(""); // Limpiamos el UUID del dueño
    }

    void setMaster(String s) {
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new TemptGoal(this, 0.4, Ingredient.of(Items.BREAD, Items.WHEAT), false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            SexAnimationTracker.serverTick(this);
        }
    }

    public void setAnimStateFiltered(AnimState next) {
        if (getAnimState() == next) return;
        this.entityData.set(ANIM_STATE, next.name());
        setAnimTick(0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("NpcUUID", this.entityData.get(NPC_UUID));
        tag.putString("MasterUUID", this.entityData.get(MASTER_UUID));
        tag.putInt("ModelIndex", getModelIndex());
        tag.putFloat("InteractionLevel", getInteractionLevel());
        tag.putDouble("homeX", homePos.x); tag.putDouble("homeY", homePos.y); tag.putDouble("homeZ", homePos.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcUUID")) this.entityData.set(NPC_UUID, tag.getString("NpcUUID"));
        this.entityData.set(MASTER_UUID, tag.getString("MasterUUID"));
        this.setModelIndex(tag.getInt("ModelIndex"));
        this.setInteractionLevel(tag.getFloat("InteractionLevel"));
        this.homePos = new Vec3(tag.getDouble("homeX"), tag.getDouble("homeY"), tag.getDouble("homeZ"));
    }

    public UUID getNpcUUID() { return UUID.fromString(this.entityData.get(NPC_UUID)); }

    // CORREGIDO: RemovalReason es una clase interna de Entity en 1.20.1
    @Override
    public void remove(Entity.RemovalReason reason) {
        ALL_ACTIVE.remove(this);
        super.remove(reason);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) { return super.getDimensions(pose).scale(getModelScale()); }

    public float getModelScale() { return 1.0F; }

    public abstract Vec3 getBonePosition(String boneName);

    public abstract void triggerAction(String action, UUID playerId);

    public void setHomePosition(Vec3 snapped) {

    }
}