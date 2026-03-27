package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.ModConstants;
import com.trolmastercard.sexmod.ModEntityRegistry;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.*;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.client.ClientStateManager;
import com.trolmastercard.sexmod.client.HornyMeterOverlay;
import com.trolmastercard.sexmod.item.GalathCoinItem;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityVelocityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * GalathEntity - Entidad de Jefe y Compañera.
 * Portado a 1.20.1 / GeckoLib 4.
 */
public class GalathEntity extends BaseNpcEntity implements NpcQueryInterface {

    // =========================================================================
    //  Constantes (Nombres originales preservados para lógica interna)
    // =========================================================================
    public static final float  WIDTH_WILD            = 0.6F;
    public static final float  HP_PHASE2             = 50.0F;
    public static final int    WAKEUP_DELAY_MS       = 8000;
    public static final float  GALATH_MASS           = 0.5F;
    public static final RgbColor GOLD_COLOR          = new RgbColor(0.83137256F, 0.6862745F, 0.21568628F);

    public static final Vec3 ENERGY_BALL_R_OFFSET = new Vec3(-1.0493420362472534D, 2.0547213554382324D, -0.05048239231109619D);
    public static final Vec3 ENERGY_BALL_L_OFFSET = new Vec3(1.2522261142730713D, 1.435773253440857D, 0.23570987582206726D);

    // =========================================================================
    //  Datos Sincronizados (SynchedData)
    // =========================================================================
    public static final EntityDataAccessor<Integer> DATA_TARGET_ID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> DATA_ANIM_IDX = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DATA_EB_R = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_EB_L = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_MIRRORED = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> DATA_ATK_ANIM = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String>  DATA_BOMB_POS = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> DATA_PARALYZED = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float>   DATA_FLOAT = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> DATA_KNOCKED = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String>  DATA_MANGLE_UUID = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> DATA_RUNNING = SynchedEntityData.defineId(GalathEntity.class, EntityDataSerializers.BOOLEAN);

    // =========================================================================
    //  Campos de Instancia
    // =========================================================================
    final ServerBossEvent bossBar = new ServerBossEvent(Component.literal("Galath"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

    // Partes del jefe (Multi-part)
    public final SelectableEntityPart hitboxRight = new SelectableEntityPart(this, "energyBallHitBox", 0.75F, 0.75F);
    public final SelectableEntityPart hitboxLeft = new SelectableEntityPart(this, "energyBallHitBox", 0.75F, 0.75F);

    public GalathAttackState combatState = null;
    public Vec3 renderPos    = null;
    public Vec3 prevRenderPos = null;
    public int summonBoneTick = 0;
    public final List<WitherSkeleton> skeletons = new ArrayList<>();

    public float  boostSpeed     = 0.0F;
    public Vec3   interpTarget   = null;
    boolean pathInvalid = false;
    Vec3 moveDelta, prevPos, velSnap;

    boolean camActive  = false;
    public int  summonTickRaw = 0;
    double leanX = 0, leanZ = 0, prevLeanX = 0, prevLeanZ = 0;
    boolean combatInited = false;
    net.minecraft.world.level.pathfinder.Path cachedPath = null;
    BlockPos wanderTarget = null;
    int wanderRetry = 0;

    int  surpriseFinishPhase = 0; // Antes rapeCumPhase
    int  knockGroundTick = 0;
    int  knockGroundWait = 0;
    long lastHurtSound   = 0L;
    boolean interactionFinishDone = false; // Antes morningCumDone
    boolean shouldDespawnOld = false;
    int  interactionVariant = 0; // Antes rapeVariant
    boolean corruptHard = false;
    public boolean swordVisible   = false;
    public boolean swordHitActive = false;
    public boolean drawStars      = true;
    public boolean nudeMode       = false;
    boolean altAudioToggle        = false; // Antes altMoanToggle

    // =========================================================================
    //  Constructores y Atributos
    // =========================================================================

    public GalathEntity(EntityType<? extends GalathEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseNpcEntity.createBaseAttributes()
                .add(Attributes.MAX_HEALTH, 110.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 50.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TARGET_ID, -1);
        this.entityData.define(DATA_ANIM_IDX, 0);
        this.entityData.define(DATA_EB_R, true);
        this.entityData.define(DATA_EB_L, true);
        this.entityData.define(DATA_MIRRORED, false);
        this.entityData.define(DATA_ATK_ANIM, -1);
        this.entityData.define(DATA_BOMB_POS, "null");
        this.entityData.define(DATA_PARALYZED, false);
        this.entityData.define(DATA_FLOAT, 0.0F);
        this.entityData.define(DATA_KNOCKED, false);
        this.entityData.define(DATA_MANGLE_UUID, "");
        this.entityData.define(DATA_RUNNING, false);
    }

    // =========================================================================
    //  Lógica de Vínculo y MangleLie
    // =========================================================================

    @Nullable
    public UUID getMangleLieUUID() {
        String s = this.entityData.get(DATA_MANGLE_UUID);
        return s.isEmpty() ? null : UUID.fromString(s);
    }

    public void setMangleLieUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_MANGLE_UUID, uuid == null ? "" : uuid.toString());
    }

    @Nullable
    public MangleLieEntity getMangleLie(boolean serverSide) {
        UUID uuid = getMangleLieUUID();
        if (uuid == null) return null;
        BaseNpcEntity npc = serverSide ? BaseNpcEntity.getByUUID(uuid) : BaseNpcEntity.getByUUIDClient(uuid);
        return (npc instanceof MangleLieEntity m) ? m : null;
    }

    // =========================================================================
    //  Lógica de Tick Principal
    // =========================================================================

    @Override
    public void tick() {
        boolean tamed = isTamed();
        if (tamed) preTickTamed(); else preTickWild();

        // Capturar velocidad para interpolación de renderizado
        this.velSnap = this.getDeltaMovement().scale(0.9D);
        this.moveDelta = this.position();
        this.prevPos = this.position().add(this.getDeltaMovement());

        super.tick();

        if (tamed) postTickTamed(); else postTickWild();
    }

    void preTickWild() {
        if (!this.entityData.get(DATA_PARALYZED)) setNoGravity(getTarget() != null);
        float max = this.getMaxHealth();
        if (max > 0) this.bossBar.setProgress(this.getHealth() / max);
        if (getAnimState() == AnimState.SUMMON_SKELETON) {
            if (++this.summonTickRaw > 45) this.summonTickRaw = 0;
        } else this.summonTickRaw = 0;
    }

    void postTickWild() {
        updateHitboxes();
        updateBodyLean();
        if (this.level().isClientSide) {
            spawnSwordParticles();
            handleInteractionIntroCamera();
        }
        if (getAnimState() != AnimState.ATTACK_SWORD) {
            this.swordVisible = false;
            this.swordHitActive = false;
        }
    }

    void updateHitboxes() {
        this.hitboxRight.setActive(false);
        this.hitboxLeft.setActive(false);
        if (this.summonTickRaw < 9.0F || this.summonTickRaw > 30.0F) return;
        this.hitboxRight.setActive(true);
        this.hitboxLeft.setActive(true);

        boolean mirrored = this.entityData.get(DATA_MIRRORED);
        Vec3 base = this.position();
        Vec3 offR = mirrored ? VectorMathUtil.mirrorX(ENERGY_BALL_R_OFFSET) : ENERGY_BALL_R_OFFSET;
        Vec3 offL = mirrored ? VectorMathUtil.mirrorX(ENERGY_BALL_L_OFFSET) : ENERGY_BALL_L_OFFSET;
        Vec3 posR = VectorMathUtil.rotateAroundY(offR, 180.0F + this.yBodyRot).add(base);
        Vec3 posL = VectorMathUtil.rotateAroundY(offL, 180.0F + this.yBodyRot).add(base);
        this.hitboxRight.moveTo(posR.x, posR.y, posR.z, this.yBodyRot, 0.0F);
        this.hitboxLeft.moveTo(posL.x, posL.y, posL.z, this.yBodyRot, 0.0F);
    }

    // =========================================================================
    //  Máquina de Estados de Animación (GeckoLib 4)
    // =========================================================================

    @Override
    public void setAnimState(AnimState newState) {
        AnimState prev = getAnimState();
        if (prev == AnimState.GALATH_DE_SUMMON) return;

        // Registro de tiempos de finalización para efectos persistentes
        if (!this.level().isClientSide && AnimState.isFinish(prev)) {
            // SexProposalManager.recordResolutionTime(getInteractionPartnerUUID(), this.level().getGameTime());
        }

        super.setAnimState(newState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "eyes", 10, event -> {
            if (getAnimState() != null && getAnimState().autoBlink) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.blink"));
            }
            return PlayState.STOP;
        }));

        reg.add(new AnimationController<>(this, "movement", 5, this::movementPredicate));

        var actionCtrl = new AnimationController<>(this, "action", 0, this::actionPredicate);
        actionCtrl.setSoundKeyframeHandler(event -> handleSoundKeyframe(event.getKeyframeData().getSound()));
        reg.add(actionCtrl);
    }

    private PlayState movementPredicate(AnimationState<GalathEntity> event) {
        if (getAnimState() != AnimState.NULL) return PlayState.STOP;
        if (!this.onGround()) return event.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.controlled_flight"));

        if (this.entityData.get(DATA_RUNNING)) return event.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.run"));
        if (event.isMoving()) return event.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.walk"));

        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.idle"));
    }

    private PlayState actionPredicate(AnimationState<GalathEntity> event) {
        AnimState state = getAnimState();
        if (state == null || state == AnimState.NULL) return PlayState.STOP;

        // Mapeo dinámico a los archivos JSON originales
        switch (state) {
            case SUMMON_SKELETON -> event.getController().setAnimation(RawAnimation.begin().thenPlay("animation.galath.summon_skeleton" + (this.entityData.get(DATA_MIRRORED) ? "Mirrored" : "")));
            case ATTACK_SWORD    -> event.getController().setAnimation(RawAnimation.begin().thenPlay("animation.galath.attack"));
            case KNOCK_OUT_FLY   -> event.getController().setAnimation(RawAnimation.begin().thenPlay("animation.galath.knockout_air"));
            case SURPRISE_INTRO  -> event.getController().setAnimation(RawAnimation.begin().thenPlay("animation.galath.rape_intro"));
            case SURPRISE_ACTIVE -> event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.galath.rape" + this.interactionVariant));
            case CORRUPT_SLOW    -> event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.galath.corrupt_slow"));
            case GIVE_COIN       -> event.getController().setAnimation(RawAnimation.begin().thenPlay("animation.galath.give_coin"));
            default -> event.getController().setAnimation(RawAnimation.begin().thenLoop("animation.galath.idle"));
        }
        return PlayState.CONTINUE;
    }

    // =========================================================================
    //  Manejador de Sonidos y Eventos SFW
    // =========================================================================

    private void handleSoundKeyframe(String snd) {
        if (!this.level().isClientSide) return;
        Vec3 p = position();

        switch (snd) {
            case "giggle", "moan", "switchmoan", "cum", "orgasm" ->
                    this.level().playLocalSound(p.x, p.y, p.z, ModSounds.randomFrom(ModSounds.GIRLS_GALATH_GIGGLE), SoundSource.HOSTILE, 1.0F, 1.0F, false);
            case "strongcharge" -> playNpcSound(ModSounds.GIRLS_GALATH_STRONGCHARGE[0]);
            case "strong_blast" -> this.level().playLocalSound(p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F, false);
            case "setNude" -> { this.nudeMode = true; /* Partículas de desvanecimiento */ }
            case "sexui" -> { if (isLocalInteractionPartner()) HornyMeterOverlay.showSexUI(); }
        }
    }

    // =========================================================================
    //  Eventos Globales (Inner Class EventHandler)
    // =========================================================================

    public static class EventHandler {

        @SubscribeEvent
        public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
            Player player = event.getEntity();
            if (player.level().isClientSide) return;

            // Lógica de "Despertar Mágico" (Antes Morning Blowjob)
            BlockPos bedPos = player.blockPosition();
            if (player.level().getBlockState(bedPos).getBlock() instanceof BedBlock) {
                // Spawn de Galath para asistencia matutina
                GalathEntity galath = new GalathEntity(ModEntityRegistry.GALATH_ENTITY.get(), player.level());
                galath.setPos(player.position());
                player.level().addFreshEntity(galath);
                galath.setAnimState(AnimState.MORNING_ACTION_SLOW);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity() instanceof GalathEntity galath) {
                if (!galath.isTamed()) {
                    galath.setHealth(1.0F);
                    galath.launchKnockOut(null);
                    event.setCanceled(true);
                }
            }
        }
    }

    // Helpers de compatibilidad 1.20.1
    public boolean isTamed() { return getOwnerUUID() != null; }
    private boolean isLocalInteractionPartner() { return Minecraft.getInstance().player != null && Minecraft.getInstance().player.getUUID().equals(getInteractionTargetUUID()); }
    public void launchKnockOut(@Nullable Vec3 origin) { this.entityData.set(DATA_PARALYZED, true); setAnimState(AnimState.KNOCK_OUT_FLY); }

    @Override public boolean isNudeMode() { return this.nudeMode; }
    @Override public boolean queryBoolValue() { return this.nudeMode; }
}