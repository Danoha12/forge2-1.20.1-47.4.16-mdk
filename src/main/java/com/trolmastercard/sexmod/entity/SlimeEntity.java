package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.FakeWorld;
import com.trolmastercard.sexmod.client.gui.HornyMeterOverlay;
import com.trolmastercard.sexmod.client.handler.ClientStateManager;
import com.trolmastercard.sexmod.client.shader.OutlineShaderManager;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModEffects;
import com.trolmastercard.sexmod.registry.ModEntityRegistry;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * SlimeEntity — Portado a 1.20.1 / GeckoLib 4.
 * * Entidad de IA personalizada sin goals Vanilla.
 * * Maneja físicas de salto estilo Slime y estados reproductivos.
 */
public class SlimeEntity extends BaseNpcEntity {

    static final double FORWARD_OFFSET = 0.7D;
    static final double LOOK_RANGE = 100.0D;
    static final float JUMP_CHANCE = 0.1F;
    static final int BIRTH_TICKS = 2400;

    public static final EntityDataAccessor<Integer> DATA_HORNY_LEVEL = SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_SAVED_YAW = SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DATA_TICKS_UNTIL_BIRTH = SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.INT);

    JumpState jumpState = JumpState.IDLE;
    int jumpTick = 0;
    boolean wasOnGround = true;
    boolean wantsToJump = false;
    int poundCount = 0;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public SlimeEntity(EntityType<? extends SlimeEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_HORNY_LEVEL, 0);
        this.entityData.define(DATA_SAVED_YAW, 0.0F);
        this.entityData.define(DATA_TICKS_UNTIL_BIRTH, -1);
    }

    @Override
    public String getNpcName() { return "Slime"; }

    @Override
    public float getWalkSpeed() { return 1.6F; }

    @Override
    protected void registerGoals() {
        // Sin IA Vanilla, usa máquina de estados propia.
    }

    // ── Transiciones de Estado ────────────────────────────────────────────────

    @Override
    public void setAnimStateFiltered(AnimState state) {
        AnimState current = getAnimState();
        if (current == AnimState.CUMBLOWJOB && (state == AnimState.THRUSTBLOWJOB || state == AnimState.SUCKBLOWJOB)) return;
        if (current == AnimState.DOGGYCUM && (state == AnimState.DOGGYFAST || state == AnimState.DOGGYSLOW)) return;
        super.setAnimStateFiltered(state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderHornyOverlay() { return false; }

    @Nullable
    @Override
    protected AnimState getCumStateFor(AnimState state) {
        if (state == AnimState.SUCKBLOWJOB || state == AnimState.THRUSTBLOWJOB) return AnimState.CUMBLOWJOB;
        if (state == AnimState.DOGGYSLOW || state == AnimState.DOGGYFAST) return AnimState.DOGGYCUM;
        return null;
    }

    @Nullable
    @Override
    protected AnimState getNextAnimStateOnNull(AnimState state) {
        if (state == AnimState.SUCKBLOWJOB) return AnimState.THRUSTBLOWJOB;
        if (state == AnimState.DOGGYSLOW) return AnimState.DOGGYFAST;
        return null;
    }

    // ── Lógica Principal ──────────────────────────────────────────────────────

    public void resetHorny() {
        this.entityData.set(DATA_HORNY_LEVEL, 0);
        this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 1);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        tickHornyApproach();
        tickBirthTimer();

        if (hasEffect(ModEffects.HORNY.get()) && this.jumpState == JumpState.IDLE) {
            if (this.entityData.get(DATA_TICKS_UNTIL_BIRTH) == -1) {
                this.entityData.set(DATA_HORNY_LEVEL, 2);
                if (this.entityData.get(BaseNpcEntity.DATA_OUTFIT_INDEX) == 1) {
                    setAnimStateFiltered(AnimState.UNDRESS);
                }
                removeEffect(ModEffects.HORNY.get());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getAnimState() == AnimState.NULL) tickJump();

        if (this.entityData.get(DATA_HORNY_LEVEL) >= 2 && this.tickCount % 10 == 0) {
            spawnHeartParticles(this);
        }

        if (this.level().isClientSide) {
            tickClientRiding();
            tickClientBirthParticle();
        }
    }

    // ── Partículas y Nacimiento ───────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    private void tickClientBirthParticle() {
        int ticksLeft = this.entityData.get(DATA_TICKS_UNTIL_BIRTH);
        if (ticksLeft == -1) return;

        // Forma correcta de spawnear partículas en 1.20.1
        this.level().addParticle(ParticleTypes.WITCH, this.getRandomX(0.5D), this.getRandomY() - 0.25D, this.getRandomZ(0.5D), 0.0D, 0.0D, 0.0D);

        if (ticksLeft == 0) playSoundEffect(ModSounds.MISC_PLOB[0]);
    }

    private void tickBirthTimer() {
        int ticksLeft = this.entityData.get(DATA_TICKS_UNTIL_BIRTH);
        if (ticksLeft == -1) return;

        this.entityData.set(DATA_TICKS_UNTIL_BIRTH, ticksLeft - 1);
        if (--ticksLeft >= 0) return;

        KoboldEgg egg = new KoboldEgg(ModEntityRegistry.KOBOLD_EGG.get(), this.level());
        egg.setPos(this.getX(), this.getY(), this.getZ());
        this.level().addFreshEntity(egg);
        this.entityData.set(DATA_TICKS_UNTIL_BIRTH, -1);
    }

    // ── Interacciones de IA ───────────────────────────────────────────────────

    private void tickHornyApproach() {
        int horny = this.entityData.get(DATA_HORNY_LEVEL);
        if (horny < 2) return;

        if (horny >= 4 && this.onGround() && getAnimState() == AnimState.NULL) {
            setPos(position());
            setYRot(this.yBodyRot);
            this.entityData.set(BaseNpcEntity.DATA_IMMOVABLE, true);
            setInvulnerable(true);
            this.noPhysics = true;
            setAnimStateFiltered(AnimState.STARTDOGGY);
            return;
        }

        Player nearby = this.level().getNearestPlayer(this, 1.0D);
        if (nearby != null && nearby.onGround() && getSexPartner(nearby) == null) {
            setPos(position());
            setYRot(this.yBodyRot);
            this.entityData.set(BaseNpcEntity.DATA_IMMOVABLE, true);
            setInvulnerable(true);
            this.noPhysics = true;

            nearby.setInvulnerable(true);
            nearby.noPhysics = true;

            Vec3 frontOffset = VectorMathUtil.rotateYaw(new Vec3(0, 0, FORWARD_OFFSET), this.yBodyRot);
            nearby.setPos(this.getX() + frontOffset.x, this.getY(), this.getZ() + frontOffset.z);
            nearby.yBodyRot = this.yBodyRot + 180.0F;

            if (nearby instanceof ServerPlayer sp) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new CameraControlPacket(false));
                setOwnerUUID(nearby.getUUID());
            }

            setAnimStateFiltered(getAnimState() == AnimState.WAITDOGGY ? AnimState.DOGGYSTART : AnimState.SUCKBLOWJOB);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tickClientRiding() {
        if (getOwnerUUID() == null || Minecraft.getInstance().player == null) return;
        LocalPlayer local = Minecraft.getInstance().player;
        if (!getOwnerUUID().equals(local.getUUID())) return;

        Vec3 offset = VectorMathUtil.rotateYaw(new Vec3(0, 0, FORWARD_OFFSET), this.yBodyRot);
        Vec3 ridePos = this.position().add(offset);
        local.setPos(ridePos.x, ridePos.y, ridePos.z);
        local.setDeltaMovement(0, 0, 0);
    }

    // ── Físicas de Salto ──────────────────────────────────────────────────────

    private void tickJump() {
        if (this.level().isClientSide) {
            if (this.jumpTick == 90) this.jumpState = JumpState.JUMP_START;
            if (!this.wasOnGround && this.onGround()) {
                this.jumpState = JumpState.JUMP_END;
                this.jumpTick = 0;
            }
            float savedYaw = this.entityData.get(DATA_SAVED_YAW);
            this.yBodyRot = savedYaw;
            this.yBodyRotO = savedYaw;
            this.yHeadRot = savedYaw;
        } else {
            if (this.jumpTick == 85) this.entityData.set(DATA_SAVED_YAW, getTargetYaw());
            if (this.jumpTick == 100) doJumpLaunch();

            if (!this.wasOnGround && this.onGround() && this.entityData.get(DATA_TICKS_UNTIL_BIRTH) == -1) {
                this.wantsToJump = this.random.nextFloat() < JUMP_CHANCE;
            }

            if (this.wantsToJump && this.jumpTick == 50) {
                int horny = this.entityData.get(DATA_HORNY_LEVEL);
                this.entityData.set(DATA_HORNY_LEVEL, horny + 1);
                if (horny + 1 == 1) setAnimStateFiltered(AnimState.UNDRESS);
            }
        }

        if (this.onGround()) this.jumpTick++;
        this.wasOnGround = this.onGround();
    }

    private void doJumpLaunch() {
        this.setDeltaMovement(0, 0, 0);
        this.getJumpControl().jump();
        float yaw = this.entityData.get(DATA_SAVED_YAW);
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        Vec3 dir = VectorMathUtil.rotateYaw(new Vec3(0, 0, FORWARD_OFFSET), yaw);
        this.setDeltaMovement(dir.x, this.getDeltaMovement().y, dir.z);
        this.jumpTick = 0;
    }

    private float getTargetYaw() {
        if (this.entityData.get(DATA_TICKS_UNTIL_BIRTH) != -1 || this.entityData.get(DATA_HORNY_LEVEL) < 2) return getRandomYaw();
        Player nearest = this.level().getNearestPlayer(this, LOOK_RANGE);
        if (nearest == null || getSexPartner(nearest) != null) return getRandomYaw();
        return (float) Math.toDegrees(Math.atan2(this.getZ() - nearest.getZ(), this.getX() - nearest.getX())) + 90.0F;
    }

    private float getRandomYaw() { return ModConstants.RAND.nextFloat() * 360.0F; }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) { return false; }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("hornyLevel", this.entityData.get(DATA_HORNY_LEVEL));
        tag.putInt("ticksUntilBirth", this.entityData.get(DATA_TICKS_UNTIL_BIRTH));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_HORNY_LEVEL, tag.getInt("hornyLevel"));
        this.entityData.set(DATA_TICKS_UNTIL_BIRTH, tag.getInt("ticksUntilBirth"));
        if (this.entityData.get(DATA_HORNY_LEVEL) != 0) {
            this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 0);
        }
        this.noPhysics = false;
        setInvulnerable(false);
    }

    // ── GeckoLib 4 Controllers ────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Controlador de Ojos
        registrar.add(new AnimationController<>(this, "eyes", 0, state -> {
            if (this.level() instanceof FakeWorld) return PlayState.STOP;
            AnimState as = getAnimState();
            String anim = (as != AnimState.NULL && as != null && as.autoBlink) ? "animation.slime.fhappy" : "animation.slime.null";
            return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
        }));

        // Controlador de Acción
        AnimationController<SlimeEntity> actionCtrl = new AnimationController<>(this, "action", 0, state -> {
            if (this.level() instanceof FakeWorld) return PlayState.STOP;
            AnimState as = getAnimState();

            if (as == AnimState.NULL || as == null) {
                return state.setAndContinue(RawAnimation.begin().thenPlay(jumpState.animName));
            }

            return switch (as) {
                case UNDRESS       -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.undress"));
                case DRESS         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.dress"));
                case STRIP         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.strip"));
                case BLOWJOBINTRO  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.blowjobintro"));
                case SUCKBLOWJOB   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.blowjobsuck"));
                case THRUSTBLOWJOB -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.blowjobthrust"));
                case CUMBLOWJOB    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.blowjobcum"));
                case DOGGYGOONBED  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggygoonbed"));
                case WAITDOGGY     -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggywait"));
                case DOGGYSTART    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggystart"));
                case DOGGYSLOW     -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggyslow"));
                case DOGGYFAST     -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggyfast"));
                case DOGGYCUM      -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggycum"));
                default            -> PlayState.STOP;
            };
        });

        // Eventos de Sonido
        actionCtrl.setSoundKeyframeHandler(event -> {
            String sound = event.getKeyframeData().getSound();
            switch (sound) {
                case "undress" -> {
                    if (isLocalPlayer()) { setNpcCustomModel("0"); setAnimStateFiltered(AnimState.NULL); }
                }
                case "dress" -> {
                    if (isLocalPlayer()) { this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 1); setAnimStateFiltered(AnimState.NULL); resetHorny(); }
                }
                case "becomeNude" -> this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 0);
                case "sexUiOn" -> { if (isLocalPlayer() && !HornyMeterOverlay.isVisible()) HornyMeterOverlay.showSexUI(); }
                case "bjiMSG10" -> { if (isLocalPlayer()) sendCameraAngle(-0.4D, -0.8D, -0.2D, 60.0F, -3.0F); }
                case "bjiMSG11" -> { playSoundEffect(SoundEvents.PLAYER_BREATH, 0.5F); if (isLocalPlayer()) HornyMeterOverlay.addHorny(0.02D); }
                case "bjiMSG12" -> { if (this.random.nextInt(5) == 0) playSoundEffect(SoundEvents.PLAYER_SPLASH, 0.5F); playSoundEffect(SoundEvents.PLAYER_BREATH, 0.5F); if (isLocalPlayer()) HornyMeterOverlay.addHorny(0.02D); }
                case "bjtMSG1" -> { playSoundEffect(SoundEvents.PLAYER_SWIM); playSoundEffect(SoundEvents.PLAYER_SMALL_FALL); if (isLocalPlayer()) HornyMeterOverlay.addHorny(0.04D); }
                case "bjiDone", "bjtDone" -> { setAnimStateFiltered(AnimState.SUCKBLOWJOB); if (isLocalPlayer()) HornyMeterOverlay.hideSexUI(); }
                case "bjtReady", "doggyfastReady" -> { if (isLocalPlayer() && ClientStateManager.isThirdPerson()) triggerNpcUiEvent(); }
                case "bjcMSG1" -> playSoundEffect(SoundEvents.PLAYER_SPLASH);
                case "bjcMSG2" -> { playSoundEffect(SoundEvents.PLAYER_SPLASH); if (isLocalPlayer()) HornyMeterOverlay.fadeOut(); }
                case "bjcBlackScreen" -> { if (isLocalPlayer()) OutlineShaderManager.doBlackScreen(); }
                case "bjcDone", "doggyCumDone" -> { if (isLocalPlayer()) { HornyMeterOverlay.hideSexUI(); resetHorny(); setNpcCustomAttribute("pregnant", String.valueOf(BIRTH_TICKS)); } }
                case "doggyGoOnBedMSG1" -> { playSoundEffect(SoundEvents.PLAYER_BREATH); this.yBodyRotO = this.yBodyRot; }
                case "doggyGoOnBedDone" -> setAnimStateFiltered(AnimState.WAITDOGGY);
                case "doggystartMSG1" -> playSoundEffect(ModSounds.MISC_TOUCH[0]);
                case "doggystartMSG2" -> playSoundEffect(ModSounds.MISC_TOUCH[1]);
                case "doggystartMSG3" -> playSoundEffect(SoundEvents.PLAYER_BREATH, 0.25F);
                case "doggystartMSG4" -> playSoundEffect(ModSounds.pickRandom(ModSounds.MISC_SMALLINSERTS), 1.5F);
                case "doggystartMSG5" -> { playSoundEffect(ModSounds.pickRandom(ModSounds.MISC_POUNDING), 0.33F); playSoundEffect(SoundEvents.PLAYER_SWIM); }
                case "doggystartDone" -> { setAnimStateFiltered(AnimState.DOGGYSLOW); if (isLocalPlayer()) HornyMeterOverlay.showSexUI(); }
                case "doggyslowMSG1" -> {
                    playSoundEffect(ModSounds.pickRandom(ModSounds.MISC_POUNDING), 0.33F);
                    playSoundEffect(this.random.nextInt(4) == 0 ? (this.random.nextInt(2) == 0 ? SoundEvents.PLAYER_SPLASH : SoundEvents.PLAYER_BREATH) : SoundEvents.PLAYER_SWIM);
                    if (isLocalPlayer()) HornyMeterOverlay.addHorny(0.02D);
                }
                case "doggyslowMSG2" -> playSoundEffect(SoundEvents.PLAYER_SWIM);
                case "doggyfastMSG1" -> {
                    playSoundEffect(ModSounds.pickRandom(ModSounds.MISC_POUNDING), 0.75F);
                    if (isLocalPlayer()) HornyMeterOverlay.addHorny(0.04D);
                    this.poundCount++;
                    playSoundEffect(this.poundCount % 2 == 0 ? (this.random.nextInt(2) == 0 ? SoundEvents.PLAYER_SPLASH : SoundEvents.PLAYER_BREATH) : SoundEvents.PLAYER_SWIM);
                }
                case "doggyfastDone" -> setAnimStateFiltered(AnimState.DOGGYSLOW);
                case "doggycumMSG1" -> { playSoundEffect(ModSounds.MISC_CUMINFLATION[0], 4.0F); playSoundEffect(ModSounds.pickRandom(ModSounds.MISC_POUNDING), 2.0F); playSoundEffect(SoundEvents.PLAYER_SMALL_FALL); }
                case "jumpStart" -> playSoundEffect(SoundEvents.PLAYER_SPLASH);
                case "jumpStartDone" -> this.jumpState = JumpState.JUMP_AIR;
                case "jumpEndSound" -> playSoundEffect(SoundEvents.PLAYER_BREATH);
                case "jumpEndDone" -> this.jumpState = JumpState.IDLE;
            }
        });

        registrar.add(actionCtrl);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    public enum JumpState {
        IDLE("animation.slime.idle"),
        JUMP_START("animation.slime.jumpstart"),
        JUMP_AIR("animation.slime.jumpair"),
        JUMP_END("animation.slime.jumpend");

        public final String animName;
        JumpState(String anim) { this.animName = anim; }
    }
}