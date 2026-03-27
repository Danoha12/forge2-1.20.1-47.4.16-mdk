package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MangleLieEntity extends BaseNpcEntity {

    public static final String MOMMY_TAG = "sexmod:mommy";

    // Constantes de gameplay
    public static final float ANIM_DURATION_TICKS = 60.0F;
    public static final float ARROW_FIRE_TICKS   = 28.0F;
    public static final float WALK_SPEED         = 0.65F;
    public static final float MAX_HP             = 700.0F;

    // Data Sync - Refactorizado a tipos nativos
    public static final EntityDataAccessor<Optional<UUID>> DATA_MOMMY_UUID =
            SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Boolean> DATA_IS_ON_BACK =
            SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_TARGET_ID =
            SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Long> DATA_SEX_START_TIME =
            SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.LONG);
    public static final EntityDataAccessor<Boolean> DATA_SCARED =
            SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable private UUID pendingMommyUUID = null;
    private boolean wasWild = true;
    private boolean shouldDespawn = false;
    private boolean arrowFired = false;

    // Flags de estado
    public float headYaw, headPitch;
    private boolean threesomeNFlag, threesomeYFlag, threesomeMFlag;
    public int cumStageIndex = 2;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public MangleLieEntity(EntityType<? extends MangleLieEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MOMMY_UUID, Optional.empty());
        this.entityData.define(DATA_IS_ON_BACK, false);
        this.entityData.define(DATA_TARGET_ID, -1);
        this.entityData.define(DATA_SEX_START_TIME, -1L);
        this.entityData.define(DATA_SCARED, false);
    }

    // -- Getters y Setters Optimizados --

    public void setOnBack(boolean value) { this.entityData.set(DATA_IS_ON_BACK, value); }
    public boolean isOnBack() { return this.entityData.get(DATA_IS_ON_BACK); }

    @Nullable
    public UUID getMommyUUID() { return this.entityData.get(DATA_MOMMY_UUID).orElse(null); }

    public void setMommyUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_MOMMY_UUID, Optional.ofNullable(uuid));
    }

    public long getSexStartTime() { return this.entityData.get(DATA_SEX_START_TIME); }

    public void setSexStartTime(long time) {
        this.entityData.set(DATA_SEX_START_TIME, time);
        this.arrowFired = false;
    }

    @Nullable
    public GalathEntity getMommy(boolean serverSide) {
        UUID uuid = getMommyUUID();
        if (uuid == null) return null;
        BaseNpcEntity npc = serverSide ? BaseNpcEntity.getByUUIDServer(uuid) : BaseNpcEntity.getByUUIDClient(uuid);
        return (npc instanceof GalathEntity galath) ? galath : null;
    }

    // -- Lógica de Servidor --

    @Override
    public void baseTick() {
        if (this.shouldDespawn) {
            this.discard();
            return;
        }
        applyQueuedMommy();
        super.baseTick();

        if (!this.level().isClientSide) {
            updateTamedState();
            tryFindMommy(); // Optimizada internamente
            checkMommyStatus();
            handleCombatLogic();
        }
    }

    private void updateTamedState() {
        boolean tamed = isOnBack();
        this.setInvulnerable(tamed);
        this.noPhysics = tamed;
    }

    /** Busca una Mommy cercana cada 1 segundo para ahorrar CPU */
    private void tryFindMommy() {
        if (isOnBack() || getMommyUUID() != null || this.tickCount % 20 != 0) return;

        AABB box = this.getBoundingBox().inflate(15.0D);
        List<GalathEntity> candidates = this.level().getEntitiesOfClass(GalathEntity.class, box,
                g -> !g.isRemoved() && g.getMang(true) == null && g.onGround());

        if (!candidates.isEmpty()) {
            GalathEntity target = candidates.get(0);
            setAnimState(AnimState.RUN);
            this.getNavigation().moveTo(target, 0.65D);
        } else if (getAnimState() == AnimState.RUN) {
            setAnimState(null);
            this.getNavigation().stop();
        }
    }

    private void handleCombatLogic() {
        if (!isOnBack()) return;

        Entity target = getTargetEntity();
        if (target == null) {
            findNewTarget();
        } else {
            if (isTargetInvalid(target, getMommy(true))) {
                setTarget(-1);
            } else {
                tryShootArrow();
                clearTargetAfterTimeout();
            }
        }
    }

    private void tryShootArrow() {
        long startTime = getSexStartTime();
        if (startTime == -1L || (float)(this.level().getGameTime() - startTime) < ARROW_FIRE_TICKS || arrowFired) return;

        GalathEntity mommy = getMommy(true);
        Entity target = getTargetEntity();
        if (mommy == null || target == null) return;

        Arrow arrow = new Arrow(this.level(), (LivingEntity) this);
        Vec3 firePos = mommy.position().add(0, 3.5D, 0);
        arrow.setPos(firePos.x, firePos.y, firePos.z);
        Vec3 dir = target.position().subtract(firePos).normalize();
        arrow.setDeltaMovement(dir.scale(4.0D));

        BaseNpcEntity.playSoundForNpc(mommy, SoundEvents.ARROW_SHOOT, true);
        this.level().addFreshEntity(arrow);
        this.arrowFired = true;
    }

    // -- Control de Animaciones --

    @Override
    public void setAnimState(AnimState state) {
        if (getAnimState() == AnimState.THREESOME_CUM &&
                (state == AnimState.THREESOME_FAST || state == AnimState.THREESOME_SLOW)) return;

        if (!this.level().isClientSide && state == AnimState.THREESOME_CUM) {
            GalathOwnershipData.recordCumTime(getOwnerUUID(), this.level().getGameTime());
        }
        super.setAnimState(state);
    }

    // -- Persistencia (NBT) --

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (getMommyUUID() != null) tag.putUUID(MOMMY_TAG, getMommyUUID());
        tag.putBoolean("sexmod:iswild", this.wasWild);
        if (this.shouldDespawn) tag.putBoolean("sexmod:despawned", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID(MOMMY_TAG)) this.pendingMommyUUID = tag.getUUID(MOMMY_TAG);
        this.wasWild = tag.getBoolean("sexmod:iswild");
        this.shouldDespawn = tag.getBoolean("sexmod:despawned");
    }

    // -- Métodos Requeridos GeckoLib --

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "face", 0, state -> {
            if (this.entityData.get(DATA_TARGET_ID) == -1) return PlayState.STOP;
            return playAnimation("animation.manglelie.angry_face", true, state);
        }));

        registrar.add(new AnimationController<>(this, "action", 0, state -> {
            AnimState as = getAnimState();
            if (as == AnimState.NULL) {
                if (isOnBack()) return PlayState.STOP;
                String anim = this.entityData.get(DATA_SCARED) ? "animation.manglelie.scared_run" :
                        (this.walkDist > this.walkDistO ? "animation.manglelie.walk" : "animation.manglelie.idle");
                return playAnimation(anim, true, state);
            }
            // ... resto de lógica de switch simplificada ...
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    @Nullable
    public Entity getTargetEntity() {
        int id = this.entityData.get(DATA_TARGET_ID);
        return id == -1 ? null : this.level().getEntity(id);
    }

    private void setTarget(int entityId) {
        this.entityData.set(DATA_TARGET_ID, entityId);
        setSexStartTime(entityId == -1 ? -1L : this.level().getGameTime());
    }

    private void checkMommyStatus() {
        if (getMommyUUID() != null) this.wasWild = false;
        if (this.wasWild) return;

        GalathEntity mommy = getMommy(true);
        if (mommy == null || mommy.isRemoved()) {
            this.discard();
        }
    }

    @Override
    public String getNpcName() { return "Manglelie"; }
}