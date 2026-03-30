package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.gui.HornyMeterOverlay;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * MangleLieEntity — Portado a 1.20.1.
 * * Entidad de soporte vinculada a Galath.
 * * Maneja combate a distancia y estados de interacción múltiple.
 */
public class MangleLieEntity extends BaseNpcEntity implements GeoEntity {

    public static final float ARROW_FIRE_TICKS = 28.0F;

    // ── Sincronización de Datos ──────────────────────────────────────────────
    public static final EntityDataAccessor<String> MOMMY_UUID = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> IS_ON_BACK = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> THREESOME_M = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> THREESOME_Y = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.BOOLEAN);

    private boolean arrowFired = false;
    private long lastShotTime = -1L;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public MangleLieEntity(EntityType<? extends MangleLieEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MOMMY_UUID, "");
        this.entityData.define(IS_ON_BACK, false);
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(THREESOME_M, false);
        this.entityData.define(THREESOME_Y, false);
    }

    // ── Lógica de Vinculación (Mommy Galath) ─────────────────────────────────

    /** Requerido por CustomModelSavedData para saber quién es el dueño del asiento */
    public UUID getSeatMasterUUID() {
        UUID mommy = getMommyUUID();
        return mommy != null ? mommy : this.getMasterUUID();
    }

    public void setMommyUUID(@Nullable UUID uuid) {
        this.entityData.set(MOMMY_UUID, uuid == null ? "" : uuid.toString());
    }

    @Nullable
    public UUID getMommyUUID() {
        String s = this.entityData.get(MOMMY_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public GalathEntity getMommy() {
        UUID uuid = getMommyUUID();
        if (uuid == null) return null;
        Entity e = this.level().isClientSide() ?
                ((net.minecraft.client.multiplayer.ClientLevel)this.level()).getEntity(this.getId()) : // Fallback cliente
                ((ServerLevel)this.level()).getEntity(uuid);
        return (e instanceof GalathEntity g) ? g : null;
    }

    public boolean isOnBack() { return this.entityData.get(IS_ON_BACK); }
    public void setOnBack(boolean val) { this.entityData.set(IS_ON_BACK, val); }

    // ── Lógica de Combate y Soporte ──────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) return;

        if (isOnBack()) {
            handleCombatSupport();
        } else {
            searchForMommy();
        }
    }

    private void handleCombatSupport() {
        Entity target = getTargetEntity();
        if (target == null || !target.isAlive()) {
            findTarget();
        } else {
            tryShoot(target);
        }
    }

    private void tryShoot(Entity target) {
        if (arrowFired) {
            if (this.level().getGameTime() > lastShotTime + 60) arrowFired = false; // Cooldown
            return;
        }

        GalathEntity mommy = getMommy();
        if (mommy != null && this.level().getGameTime() % 40 == 0) {
            Arrow arrow = new Arrow(this.level(), this);
            Vec3 pos = mommy.position().add(0, 3.0, 0); // Dispara desde arriba de Galath
            arrow.setPos(pos.x, pos.y, pos.z);

            Vec3 dir = target.position().add(0, target.getEyeHeight(), 0).subtract(pos).normalize();
            arrow.setDeltaMovement(dir.scale(2.5));

            this.level().addFreshEntity(arrow);
            this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F);
            this.arrowFired = true;
            this.lastShotTime = this.level().getGameTime();
        }
    }

    private void findTarget() {
        AABB area = this.getBoundingBox().inflate(15);
        List<Monster> enemies = this.level().getEntitiesOfClass(Monster.class, area);
        if (!enemies.isEmpty()) {
            this.entityData.set(TARGET_ID, enemies.get(0).getId());
        }
    }

    @Nullable
    public Entity getTargetEntity() {
        int id = this.entityData.get(TARGET_ID);
        return id == -1 ? null : this.level().getEntity(id);
    }

    private void searchForMommy() {
        if (this.tickCount % 20 == 0) {
            this.level().getEntitiesOfClass(GalathEntity.class, this.getBoundingBox().inflate(10)).stream()
                    .filter(g -> g.getMangleLie() == null)
                    .findFirst()
                    .ifPresent(g -> {
                        this.getNavigation().moveTo(g, 0.5);
                        if (this.distanceTo(g) < 1.5) {
                            this.setOnBack(true);
                            g.setMangleLie(this);
                            this.setAnimStateFiltered(AnimState.RIDE_MOMMY_HEAD);
                        }
                    });
        }
    }

    // ── GeckoLib 4: Controladores ────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "main", 5, state -> {
            AnimState as = getAnimState();

            if (isOnBack()) {
                if (as == AnimState.THREESOME_SLOW) {
                    String anim = this.entityData.get(THREESOME_M) ? "animation.shared.double_holding_back" : "animation.shared.double_holding_slow";
                    return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
                }
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.manglelie.sit_on_galath"));
            }

            if (state.isMoving()) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.manglelie.walk"));
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.manglelie.idle"));
        }).setSoundKeyframeHandler(event -> {
            if (event.getKeyframeData().getSound().equals("pound")) {
                this.playSound(ModSounds.MISC_POUNDING.get(), 1.0F, 1.0F);
                if (this.level().isClientSide()) HornyMeterOverlay.addValue(0.02);
            }
        }));
    }

    @Override public void triggerAction(String action, UUID playerId) {}
    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 0.5, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}