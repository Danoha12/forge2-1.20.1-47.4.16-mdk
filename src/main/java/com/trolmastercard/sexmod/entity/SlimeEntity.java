package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public class SlimeEntity extends BaseNpcEntity {

    // -- Constants -------------------------------------------------------------

    static final double FORWARD_OFFSET   = 0.7D;
    static final float  WALK_SPEED       = 0.9F;
    static final double LOOK_RANGE       = 100.0D;
    static final float  JUMP_CHANCE      = 0.1F;
    static final int    BIRTH_TICKS      = 2400;

    // -- Synced data (Enmascarado a SFW) ---------------------------------------

    public static final EntityDataAccessor<Integer> DATA_AFFECTION_LEVEL =
            SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_SAVED_YAW =
            SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Integer> DATA_TICKS_UNTIL_BIRTH =
            SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Optional<UUID>> DATA_PARTNER_UUID =
            SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.OPTIONAL_UUID);

    // -- Instance fields -------------------------------------------------------

    JumpState jumpState   = JumpState.IDLE;
    int  jumpTick         = 0;
    boolean wasOnGround   = true;
    boolean wantsToJump   = false;
    int  interactionCount = 0;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public SlimeEntity(EntityType<? extends SlimeEntity> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_AFFECTION_LEVEL, 0);
        this.entityData.define(DATA_SAVED_YAW, 0.0F);
        this.entityData.define(DATA_TICKS_UNTIL_BIRTH, -1);
        this.entityData.define(DATA_PARTNER_UUID, Optional.empty());
    }

    @Override
    public String getNpcName() { return "Slime"; }

    @Override
    public float getWalkSpeed() { return 1.6F; }

    @Override
    protected void registerGoals() {} // Slime uses no vanilla AI goals

    // -- AnimState overrides (Estados Seguros) ---------------------------------

    @Override
    public void setAnimState(AnimState state) {
        if (getAnimState() == AnimState.GIFT_REWARD) {
            if (state == AnimState.GAME_CHASE || state == AnimState.GIFT_LOOP) return;
        }
        if (getAnimState() == AnimState.DANCE_FINISH) {
            if (state == AnimState.DANCE_FAST || state == AnimState.DANCE_SLOW) return;
        }
        super.setAnimState(state);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean shouldRenderHornyOverlay() { return false; }

    @Nullable
    @Override
    protected AnimState getCumStateFor(AnimState state) {
        if (state == AnimState.GIFT_LOOP)  return AnimState.GIFT_REWARD;
        if (state == AnimState.GAME_CHASE) return AnimState.GIFT_REWARD;
        if (state == AnimState.DANCE_SLOW) return AnimState.DANCE_FINISH;
        if (state == AnimState.DANCE_FAST) return AnimState.DANCE_FINISH;
        return null;
    }

    @Nullable
    @Override
    protected AnimState getNextAnimStateOnNull(AnimState state) {
        if (state == AnimState.GIFT_LOOP)  return AnimState.GAME_CHASE;
        if (state == AnimState.DANCE_SLOW) return AnimState.DANCE_FAST;
        return null;
    }

    @Override
    protected ResourceLocation getAmbientSoundLocation() {
        return SlimeModel.AMBIENT_SOUND;
    }

    public void resetInteraction() {
        this.entityData.set(DATA_AFFECTION_LEVEL, 0);
        this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 1);
    }

    // -- Main tick -------------------------------------------------------------

    @Override
    public void baseTick() {
        super.baseTick();
        tickApproachLogic();
        tickBirthTimer();

        if (hasMobEffect(ModEffects.HORNY.get()) && this.jumpState == JumpState.IDLE) {
            if (this.entityData.get(DATA_TICKS_UNTIL_BIRTH) == -1) {
                this.entityData.set(DATA_AFFECTION_LEVEL, 2);
                if (this.entityData.get(BaseNpcEntity.DATA_OUTFIT_INDEX) == 1) {
                    setAnimState(AnimState.UNDRESS);
                }
                removeMobEffect(ModEffects.HORNY.get());
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (getAnimState() == AnimState.NULL) tickJump();

        if (this.entityData.get(DATA_AFFECTION_LEVEL) >= 2 && this.tickCount % 10 == 0) {
            spawnHeartParticles(this);
        }

        if (this.level().isClientSide) {
            tickClientRiding();
            tickClientBirthParticle();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void tickClientBirthParticle() {
        int ticksLeft = this.entityData.get(DATA_TICKS_UNTIL_BIRTH);
        if (ticksLeft == -1) return;
        spawnParticle(net.minecraft.core.particles.ParticleTypes.WITCH, this);
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

    private void tickApproachLogic() {
        int affection = this.entityData.get(DATA_AFFECTION_LEVEL);
        if (affection < 2) return;

        if (affection >= 4 && this.onGround() && getAnimState() == AnimState.NULL) {
            lockEntityPosition();
            setAnimState(AnimState.DANCE_INIT);
            return;
        }

        Player nearby = this.level().getNearestPlayer(this, 1.0D);
        if (nearby != null && nearby.onGround() && getInteractionPartner(nearby) == null) {
            lockEntityPosition();

            nearby.setInvulnerable(true);
            nearby.noPhysics = true;

            Vec3 frontOffset = VectorMathUtil.rotateYaw(new Vec3(0, 0, FORWARD_OFFSET), this.yBodyRot);
            nearby.setPos(this.getX() + frontOffset.x, this.getY(), this.getZ() + frontOffset.z);
            nearby.setYRot(this.yBodyRot + 180.0F);

            if (nearby instanceof ServerPlayer sp) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new CameraControlPacket(false));
                this.entityData.set(DATA_PARTNER_UUID, Optional.of(nearby.getUUID()));
            }

            setAnimState(getAnimState() == AnimState.REST_WAIT ? AnimState.DANCE_START : AnimState.GIFT_LOOP);
        }
    }

    private void lockEntityPosition() {
        setPos(position());
        this.entityData.set(BaseNpcEntity.DATA_IMMOVABLE, true);
        setInvulnerable(true);
        this.noPhysics = true;
    }

    @OnlyIn(Dist.CLIENT)
    private void tickClientRiding() {
        Optional<UUID> partnerOpt = this.entityData.get(DATA_PARTNER_UUID);
        if (partnerOpt.isEmpty()) return;

        LocalPlayer local = Minecraft.getInstance().player;
        if (local == null || !partnerOpt.get().equals(local.getUUID())) return;

        Vec3 offset = VectorMathUtil.rotateYaw(new Vec3(0, 0, FORWARD_OFFSET), this.yBodyRot);
        Vec3 ridePos = this.position().add(offset);
        local.setPos(ridePos.x, ridePos.y, ridePos.z);
        local.setDeltaMovement(0, 0, 0);
    }

    // -- Jump state machine ----------------------------------------------------

    private void tickJump() {
        if (this.level().isClientSide) {
            if (this.jumpTick == 90) this.jumpState = JumpState.JUMP_START;
            if (!this.wasOnGround && this.onGround()) {
                this.jumpState = JumpState.JUMP_END;
                this.jumpTick = 0;
            }
            float savedYaw = this.entityData.get(DATA_SAVED_YAW);
            this.yBodyRot = this.yBodyRotO = this.yHeadRot = savedYaw;
        } else {
            if (this.jumpTick == 85) this.entityData.set(DATA_SAVED_YAW, getTargetYaw());
            if (this.jumpTick == 100) doJumpLaunch();

            if (!this.wasOnGround && this.onGround()) {
                if (this.entityData.get(DATA_TICKS_UNTIL_BIRTH) == -1) {
                    this.wantsToJump = this.random.nextFloat() < JUMP_CHANCE;
                }
            }
            if (this.wantsToJump && this.jumpTick == 50) {
                int next = this.entityData.get(DATA_AFFECTION_LEVEL) + 1;
                this.entityData.set(DATA_AFFECTION_LEVEL, next);
                if (next == 1) setAnimState(AnimState.UNDRESS);
            }
        }
        if (this.onGround()) this.jumpTick++;
        this.wasOnGround = this.onGround();
    }

    private void doJumpLaunch() {
        this.setDeltaMovement(0, 0, 0);
        this.getJumpControl().jump();
        float yaw = this.entityData.get(DATA_SAVED_YAW);
        Vec3 dir = VectorMathUtil.rotateYaw(new Vec3(0, 0, FORWARD_OFFSET), yaw);
        this.setDeltaMovement(dir.x, this.getDeltaMovement().y, dir.z);
        this.jumpTick = 0;
    }

    private float getTargetYaw() {
        int affection = this.entityData.get(DATA_AFFECTION_LEVEL);
        if (this.entityData.get(DATA_TICKS_UNTIL_BIRTH) != -1 || affection < 2) return getRandomYaw();

        Player nearest = this.level().getNearestPlayer(this, LOOK_RANGE);
        if (nearest == null || getInteractionPartner(nearest) != null) return getRandomYaw();

        return (float) Math.toDegrees(Math.atan2(this.getZ() - nearest.getZ(), this.getX() - nearest.getX())) + 90.0F;
    }

    private float getRandomYaw() {
        return ModConstants.RAND.nextFloat() * 360.0F;
    }

    @Override
    public void causeFallDamage(float fallDist, float damageMultiplier, DamageSource src) {}

    // -- NBT -------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("affectionLevel", this.entityData.get(DATA_AFFECTION_LEVEL));
        tag.putInt("ticksUntilBirth", this.entityData.get(DATA_TICKS_UNTIL_BIRTH));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_AFFECTION_LEVEL, tag.getInt("affectionLevel"));
        this.entityData.set(DATA_TICKS_UNTIL_BIRTH, tag.getInt("ticksUntilBirth"));
        if (this.entityData.get(DATA_AFFECTION_LEVEL) != 0) {
            this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 0);
        }
        this.noPhysics = false;
        setInvulnerable(false);
    }

    // -- GeckoLib4 (Filtro Dinámico de Sonidos Integrado) ----------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "eyes", 0, state -> {
            if (this.level() instanceof FakeWorld) return PlayState.STOP;
            AnimState as = getAnimState();
            return playAnimation(as != AnimState.NULL && as.autoBlink ? "animation.slime.fhappy" : "animation.slime.null", true, state);
        }));

        AnimationController<SlimeEntity> actionCtrl = new AnimationController<>(this, "action", 0, state -> {
            if (this.level() instanceof FakeWorld) return PlayState.STOP;
            AnimState as = getAnimState();
            if (as == AnimState.NULL) return playAnimation(jumpState.animName, true, state);

            // Mapeo JSON original respetado
            switch (as) {
                case UNDRESS       -> playAnimation("animation.slime.undress",      false, state);
                case DRESS         -> playAnimation("animation.slime.dress",        false, state);
                case STRIP         -> playAnimation("animation.slime.strip",        false, state);
                case GIFT_INTRO    -> playAnimation("animation.slime.blowjobintro", false, state);
                case GIFT_LOOP     -> playAnimation("animation.slime.blowjobsuck",  true,  state);
                case GAME_CHASE    -> playAnimation("animation.slime.blowjobthrust",true,  state);
                case GIFT_REWARD   -> playAnimation("animation.slime.blowjobcum",   false, state);
                case REST_SETUP    -> playAnimation("animation.slime.doggygoonbed", false, state);
                case REST_WAIT     -> playAnimation("animation.slime.doggywait",    true,  state);
                case DANCE_START   -> playAnimation("animation.slime.doggystart",   false, state);
                case DANCE_SLOW    -> playAnimation("animation.slime.doggyslow",    true,  state);
                case DANCE_FAST    -> playAnimation("animation.slime.doggyfast",    true,  state);
                case DANCE_FINISH  -> playAnimation("animation.slime.doggycum",     false, state);
                default            -> { return PlayState.STOP; }
            }
            return PlayState.CONTINUE;
        });

        // Centralita de sonidos inocentes
        actionCtrl.setSoundKeyframeHandler(event -> {
            String sound = event.getKeyframeData().getSound();
            switch (sound) {
                case "undress" -> {
                    if (isLocalPlayer()) { setNpcCustomModel("0"); setAnimState(AnimState.NULL); }
                }
                case "dress" -> {
                    if (isLocalPlayer()) { this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 1); setAnimState(null); resetInteraction(); }
                }
                case "becomeNude" -> this.entityData.set(BaseNpcEntity.DATA_OUTFIT_INDEX, 0);
                case "sexUiOn" -> { if (isLocalPlayer() && !InteractionMeterOverlay.isVisible()) InteractionMeterOverlay.showUI(); }

                // Filtro Dinámico aplicado a los Keyframes
                case "bjiMSG10" -> { if (isLocalPlayer()) sendCameraAngle(-0.4D, -0.8D, -0.2D, 60.0F, -3.0F); }
                case "bjiMSG11", "doggyGoOnBedMSG1" -> {
                    playSoundEffect(SoundEvents.PLAYER_BREATH, 0.5F); // SIGH
                    if (isLocalPlayer()) InteractionMeterOverlay.addProgress(0.02D);
                }
                case "bjiMSG12", "doggyfastMSG1" -> {
                    playSoundEffect(this.random.nextBoolean() ? SoundEvents.PLAYER_SPLASH : SoundEvents.PLAYER_BREATH, 0.5F); // GIGGLE/SIGH
                    if (isLocalPlayer()) InteractionMeterOverlay.addProgress(0.02D);
                }
                case "bjtMSG1" -> {
                    playSoundEffect(SoundEvents.PLAYER_SWIM);
                    if (isLocalPlayer()) InteractionMeterOverlay.addProgress(0.04D);
                }
                case "bjiDone", "bjtDone" -> { setAnimState(AnimState.GIFT_LOOP); if (isLocalPlayer()) InteractionMeterOverlay.hideUI(); }
                case "bjtReady", "doggyfastReady" -> { if (isLocalPlayer() && ClientStateManager.isThirdPerson()) triggerNpcUiEvent(); }
                case "bjcMSG1", "bjcMSG2" -> { playSoundEffect(SoundEvents.PLAYER_SPLASH); if (isLocalPlayer()) InteractionMeterOverlay.fadeOut(); }
                case "bjcBlackScreen" -> { if (isLocalPlayer()) OutlineShaderManager.doBlackScreen(); }
                case "bjcDone", "doggyCumDone" -> {
                    if (isLocalPlayer()) {
                        InteractionMeterOverlay.hideUI();
                        resetInteraction();
                        setNpcCustomAttribute("pregnant", String.valueOf(BIRTH_TICKS));
                    }
                }
                case "doggyGoOnBedDone" -> setAnimState(AnimState.REST_WAIT);
                case "doggystartMSG1", "doggystartMSG2" -> playSoundEffect(ModSounds.MISC_TOUCH[0]);
                case "doggystartMSG3" -> playSoundEffect(SoundEvents.PLAYER_BREATH, 0.25F);
                case "doggystartMSG4", "doggystartMSG5" -> playSoundEffect(SoundEvents.PLAYER_SWIM);
                case "doggystartDone" -> { setAnimState(AnimState.DANCE_SLOW); if (isLocalPlayer()) InteractionMeterOverlay.showUI(); }
                case "doggyslowMSG1", "doggyslowMSG2" -> {
                    playSoundEffect(SoundEvents.PLAYER_SWIM);
                    if (isLocalPlayer()) InteractionMeterOverlay.addProgress(0.02D);
                }
                case "doggyfastDone" -> setAnimState(AnimState.DANCE_SLOW);
                case "doggycumMSG1" -> playSoundEffect(SoundEvents.PLAYER_SMALL_FALL);

                // Jump sounds
                case "jumpStart"     -> playSoundEffect(SoundEvents.PLAYER_SPLASH);
                case "jumpStartDone" -> this.jumpState = JumpState.JUMP_AIR;
                case "jumpEndSound"  -> playSoundEffect(SoundEvents.PLAYER_BREATH);
                case "jumpEndDone"   -> this.jumpState = JumpState.IDLE;
            }
        });

        registrar.add(actionCtrl);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }

    public enum JumpState {
        IDLE("animation.slime.idle"),
        JUMP_START("animation.slime.jumpstart"),
        JUMP_AIR("animation.slime.jumpair"),
        JUMP_END("animation.slime.jumpend");

        public final String animName;
        JumpState(String anim) { this.animName = anim; }
    }
}