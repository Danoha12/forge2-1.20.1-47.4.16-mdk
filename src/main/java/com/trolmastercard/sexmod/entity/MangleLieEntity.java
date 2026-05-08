package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.gui.HornyMeterOverlay;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SpawnEnergyBallParticlesPacket; // Asegúrate de que exista
import com.trolmastercard.sexmod.util.ModUtil;
import com.trolmastercard.sexmod.util.NpcWorldUtil;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.core.animation.RawAnimation;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MangleLieEntity extends BaseNpcEntity implements GeoEntity {

    public static final String MOMMY_TAG = "sexmod:mommy";

    // Constants
    public static final float ANIM_DURATION_TICKS = 60.0F;
    public static final float ATTACK_RANGE       = 4.0F;
    public static final float ATTACK_RANGE_SQ    = 3.5F;
    public static final float ARROW_FIRE_TICKS   = 28.0F;
    public static final float SIGHT_RANGE        = 15.0F;
    public static final float SIGHT_RANGE_Y      = 15.0F;
    public static final float WALK_SPEED         = 0.65F;
    public static final float RIDE_SPEED         = 3.65F;
    public static final float MAX_DIST           = 6.0F;
    public static final float RUN_SPEED          = 80.0F;
    public static final float MAX_HP             = 700.0F;

    // Synced data
    public static final EntityDataAccessor<String>  DATA_MOMMY_UUID = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> DATA_IS_ON_BACK = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String>  DATA_SEX_START_TIME = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> DATA_SCARED = SynchedEntityData.defineId(MangleLieEntity.class, EntityDataSerializers.BOOLEAN);

    // Fields
    @Nullable private UUID pendingMommyUUID = null;

    public boolean hasTarget     = true;
    public Vec3    ridePos       = Vec3.ZERO;
    public float   rideRotY      = 0.0F;

    boolean wasWild      = true;
    boolean shouldDespawn= false;
    boolean arrowFired   = false;

    public float headYaw   = 0.0F;
    public float headPitch = 0.0F;

    boolean threesomeNFlag    = false;
    boolean threesomeYFlag    = false;
    boolean threesomeMFlag    = false;

    public int cumStageIndex  = 2;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // -- Constructor ----------------------------------------------------------

    public MangleLieEntity(EntityType<? extends MangleLieEntity> type, Level world) {
        super(type, world);
    }

    // -- Data -----------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_MOMMY_UUID,    "");
        this.entityData.define(DATA_IS_ON_BACK,    false);
        this.entityData.define(DATA_TARGET_ID,     -1);
        this.entityData.define(DATA_SEX_START_TIME,"");
        this.entityData.define(DATA_SCARED,        false);
    }

    @Override
    public void tick() {
        super.tick(); // Siempre llamamos al padre primero

        if (this.shouldDespawn) {
            this.discard();
            return;
        }

        // Toda esta lógica salvaje y de combate solo debe correr en el servidor
        if (!this.level().isClientSide()) {
            applyQueuedMommy();
            checkMommyAlive();
            updateTamedState();
            tryFindMommy();
            clearTargetIfInvalid();
            findNewTarget();
            clearTargetAfterTimeout();
            tryShootArrow();
            checkOwnedByMommy();
            checkHasMommy();
        }
    }

    // -- Ownership -------------------------------------------------------------

    public void setOnBack(boolean value) { this.entityData.set(DATA_IS_ON_BACK, value); }
    public boolean isOnBack() { return this.entityData.get(DATA_IS_ON_BACK); }

    @Nullable
    public UUID getMommyUUID() {
        String str = this.entityData.get(DATA_MOMMY_UUID);
        if ("".equals(str)) return null;
        try { return UUID.fromString(str); }
        catch (Exception e) { return null; }
    }

    public boolean isWild() { return !isOnBack(); }

    @Nullable
    public GalathEntity getMommy(boolean serverSide) {
        UUID uuid = getMommyUUID();
        if (uuid == null) return null;
        // Forma estándar de 1.20.1 en lugar del método viejo de BaseNpcEntity
        Entity e = serverSide ? ((ServerLevel)this.level()).getEntity(uuid) : this.level().getEntity(this.getId());
        return (e instanceof GalathEntity g) ? g : null;
    }

    public void setMommyUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_MOMMY_UUID, uuid == null ? "" : uuid.toString());
    }

    // -- Target entity ---------------------------------------------------------

    @Nullable
    public Entity getTargetEntity() {
        int id = this.entityData.get(DATA_TARGET_ID);
        if (id == -1) return null;
        return this.level().getEntity(id);
    }

    private void setTarget(int entityId) {
        this.entityData.set(DATA_TARGET_ID, entityId);
        setSexStartTime(entityId == -1 ? -1L : this.level().getGameTime());
    }

    public long getSexStartTime() {
        String str = this.entityData.get(DATA_SEX_START_TIME);
        if ("".equals(str)) return -1L;
        try { return Long.parseLong(str); }
        catch (Exception e) { return -1L; }
    }

    public void setSexStartTime(long time) {
        this.entityData.set(DATA_SEX_START_TIME, Long.toString(time));
        this.arrowFired = false;
    }

    // -- Server tick -----------------------------------------------------------

    @Override
    public void baseTick() {
        if (this.shouldDespawn) {
            this.discard();
            return;
        }
        applyQueuedMommy();
        checkMommyAlive();
        super.baseTick();
        updateTamedState();
        tryFindMommy();
        clearTargetIfInvalid();
        findNewTarget();
        clearTargetAfterTimeout();
        tryShootArrow();
        checkOwnedByMommy();
        checkHasMommy();
    }

    private void checkHasMommy() {
        if (getMommyUUID() != null) this.wasWild = false;
        if (this.wasWild) return;
        if (getMommy(true) == null) {
            this.discard();
        }
    }

    private void checkOwnedByMommy() {
        GalathEntity mommy = getMommy(true);
        if (mommy == null) return;
        // if (mommy.getCurrentMangUUID() == null) return; // Descomenta si existe en GalathEntity
        // if (this.getUUID().equals(mommy.getCurrentMangUUID())) return;
        // this.discard();
    }

    private void updateTamedState() {
        boolean tamed = getMommyUUID() != null;
        setInvulnerable(tamed);
        this.noPhysics = tamed;
    }

    @Override
    public boolean isInvisibleTo(Player player) { return getMommyUUID() == null; }

    private void applyQueuedMommy() {
        if (this.pendingMommyUUID == null) return;
        Entity npc = ((ServerLevel)this.level()).getEntity(this.pendingMommyUUID);
        if (!(npc instanceof GalathEntity mommy)) return;
        setMommyUUID(this.pendingMommyUUID);
        setOnBack(true);
        setAnimStateFiltered(AnimState.RIDE_MOMMY_HEAD); // Cambiado a Filtered para GeckoLib
        this.pendingMommyUUID = null;
    }

    private void checkMommyAlive() {
        if (!isOnBack()) return;
        if (!AnimState.anyOf(getAnimState(), AnimState.THREESOME_SLOW, AnimState.THREESOME_CUM, AnimState.THREESOME_FAST)) return;
        GalathEntity mommy = getMommy(true);
        if (mommy == null) return;
        if (!mommy.isRemoved()) {
            setYRot(0.0F);
            setPos(mommy.position());
            setInvulnerable(true);
            return;
        }
        this.discard();
    }

    private void tryFindMommy() {
        if (isOnBack() || getMommyUUID() != null) return;
        BlockPos pos = this.blockPosition();
        AABB box = new AABB(pos.offset(-15, -15, -15), pos.offset(15, 15, 15));
        List<GalathEntity> galaths = this.level().getEntitiesOfClass(GalathEntity.class, box);
        GalathEntity candidate = null;
        for (GalathEntity g : galaths) {
            if (g.isRemoved()) continue;
            // 🚨 REPARACIÓN 1: Añadimos 'false' (o ajusta al booleano que GalathEntity requiera)
            if (g.getMangleLie(false) != null) continue;
            if (!g.onGround()) continue;
            candidate = g;
        }
        if (candidate == null) {
            if (getAnimState() == AnimState.RUN) {
                setAnimStateFiltered(AnimState.NULL);
                this.getNavigation().stop();
            }
            return;
        }
        if (getAnimState() == AnimState.RIDE_MOMMY_HEAD) return;
        setAnimStateFiltered(AnimState.RUN);
        Vec3 diff = candidate.position().subtract(position());
        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0F;
        setYRot(yaw);
        this.getNavigation().stop();
        this.getNavigation().moveTo(candidate, 0.65D);
    }

    private void tryShootArrow() {
        long startTime = getSexStartTime();
        if (startTime == -1L) return;
        long current = this.level().getGameTime();
        if ((float) current < ARROW_FIRE_TICKS + (float) startTime) return;
        if (this.arrowFired) return;
        Entity target = getTargetEntity();
        if (target == null) return;
        GalathEntity mommy = getMommy(true);
        if (mommy == null) return;

        Arrow arrow = new Arrow(this.level(), this);
        Vec3 firePos = mommy.position().add(0, 3.5D, 0);
        arrow.setPos(firePos.x, firePos.y, firePos.z);
        Vec3 dir = target.position().subtract(firePos).normalize();
        arrow.setDeltaMovement(dir.x * 4.0D, dir.y * 4.0D, dir.z * 4.0D);
        this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F); // Usamos playSound estándar
        this.level().addFreshEntity(arrow);
        this.arrowFired = true;
    }

    private void clearTargetIfInvalid() {
        Entity target = getTargetEntity();
        if (target == null) return;
        GalathEntity mommy = getMommy(true);
        if (mommy == null || !isOnBack() || isTargetInvalid(target, mommy)) setTarget(-1);
    }

    public static boolean isTargetInvalid(Entity target, GalathEntity mommy) {
        if (target.isRemoved() || target.level() != mommy.level() || !target.isAlive()) return true;
        Vec3 diff = target.position().subtract(mommy.position());
        if (diff.x * diff.x + diff.z * diff.z > 225.0D) return true;
        float yaw = mommy.yBodyRot;
        Vec3 relative = VectorMathUtil.rotateYaw(diff, yaw); // Asume que este método tuyo existe
        return relative.z < 0.0D;
    }

    private void findNewTarget() {
        if (getTargetEntity() != null || !isOnBack()) return;
        GalathEntity mommy = getMommy(true);
        if (mommy == null || mommy.getSexPartnerUUID() != null || mommy.getAnimState() == AnimState.MASTERBATE) return;

        AABB box = new AABB(mommy.blockPosition()).inflate(15, 15, 15);
        List<Monster> mobs = this.level().getEntitiesOfClass(Monster.class, box);
        for (Monster mob : mobs) {
            if (!isTargetInvalid(mob, mommy)) {
                setTarget(mob.getId());
                return;
            }
        }
    }

    private void clearTargetAfterTimeout() {
        if (getTargetEntity() == null) return;
        long startTime = getSexStartTime();
        if (startTime == -1L) return;
        long elapsed = this.level().getGameTime() - startTime;
        if ((float) elapsed < ANIM_DURATION_TICKS) return;
        this.arrowFired = false;
        setTarget(-1);
    }

    // -- NBT -------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID uuid = getMommyUUID();
        tag.putString(MOMMY_TAG, uuid == null ? "" : uuid.toString());
        tag.putBoolean("sexmod:iswild", this.wasWild);
        if (this.shouldDespawn) tag.putBoolean("sexmod:despawned", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        String str = tag.getString(MOMMY_TAG);
        if (!"".equals(str)) {
            try { this.pendingMommyUUID = UUID.fromString(str); }
            catch (Exception ignored) {}
        }
        if (tag.getBoolean("sexmod:despawned")) this.shouldDespawn = true;
        this.wasWild = tag.getBoolean("sexmod:iswild");
    }

    // -- GeckoLib4 -------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "action", 0, state -> {
            AnimState as = getAnimState();
            if (as == AnimState.NULL) {
                if (!isOnBack()) {
                    if (state.isMoving()) {
                        return state.setAndContinue(RawAnimation.begin().thenLoop(this.entityData.get(DATA_SCARED) ? "animation.manglelie.scared_run" : "animation.manglelie.walk"));
                    }
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.manglelie.idle"));
                }
                return PlayState.STOP;
            }

            switch (as) {
                case RUN             -> { return state.setAndContinue(RawAnimation.begin().thenLoop("animation.manglelie.running")); }
                case RIDE_MOMMY_HEAD -> { return state.setAndContinue(RawAnimation.begin().thenLoop("animation.manglelie.sit_on_galath")); }
                case THREESOME_SLOW  -> { return state.setAndContinue(RawAnimation.begin().thenLoop(this.threesomeMFlag ? "animation.shared.double_holding_back" : "animation.shared.double_holding_slow")); }
                case THREESOME_FAST  -> { return state.setAndContinue(RawAnimation.begin().thenLoop(this.threesomeYFlag ? "animation.shared.double_holding_hard" : "animation.shared.double_holding_soft")); }
                case THREESOME_CUM   -> { return state.setAndContinue(RawAnimation.begin().thenPlay("animation.shared.double_holding_cum")); }
                default              -> { return PlayState.STOP; }
            }
        }).setSoundKeyframeHandler(event -> {
            String sound = event.getKeyframeData().getSound();
            switch (sound) {
                case "pound":
                    // 🚨 REPARACIÓN 2: Usamos el método nativo de sonidos (el tuyo propio que forjamos en NpcInventoryBase/BaseNpcEntity)
                    this.playSound(ModSounds.MISC_POUNDING[0].get(), 1.0F, 1.0F);
                    // Asegúrate de que HornyMeterOverlay tenga el método addValue o addHorny
                    // Si falla, cambia addValue por addHorny o viceversa según cómo lo tengas en esa clase.
                    if (this.level().isClientSide()) HornyMeterOverlay.addHorny(0.02D);
                    break;
                case "cs0": this.cumStageIndex = 0; break;
                case "cs1": this.cumStageIndex = 1; break;
                case "cs2": this.cumStageIndex = 2; break;
            }
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void triggerAction(String action, UUID playerId) {
        // Vacío, pero con sus llaves de apertura y cierre correctas
    }
    @Override
    public Vec3 getBonePosition(String boneName) {
        // Le damos un pequeño offset hacia arriba para que los cálculos de vista funcionen bien
        return this.position().add(0, 0.5, 0);
    }
    // -- Static event-handler inner class -------------------------------------

    @Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EventHandler {

        @SubscribeEvent
        @SuppressWarnings("removal")
        public static void onArrowImpact(ProjectileImpactEvent event) {
            if (!(event.getProjectile() instanceof Arrow arrow)) return;
            if (!(arrow.getOwner() instanceof MangleLieEntity)) return;
            if (event.getRayTraceResult() instanceof EntityHitResult ehr) {
                if (ehr.getEntity() instanceof BaseNpcEntity) {
                    event.setCanceled(true);
                }
            }
        }
    }

}