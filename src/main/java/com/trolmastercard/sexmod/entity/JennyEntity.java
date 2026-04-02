package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.entity.GalathSexEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.util.TickableCallback;
import com.trolmastercard.sexmod.client.handler.ClientStateManager;
import com.trolmastercard.sexmod.client.gui.HornyMeterOverlay;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.RequestRidingPacket;
import com.trolmastercard.sexmod.network.packet.SetPlayerForNpcPacket;
import com.trolmastercard.sexmod.network.packet.StartGalathSexPacket;
import com.trolmastercard.sexmod.network.packet.TeleportPlayerPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Jenny NPC entity - the main companion girl.
 * Ported from ex.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Inherits inventory from {@link NpcInventoryEntity} and implements
 * {@link TickableCallback} + {@link GalathSexEntity} interfaces.
 *
 * Animations: blowjob, boobjob (paizuri), doggy-style, strip/dress-up.
 */
public class JennyEntity extends NpcInventoryEntity
        implements TickableCallback {

    // -- Constants -------------------------------------------------------------

    // Hit-box: width=0.49, height=1.95, eye height=1.64
    private static final float WIDTH  = 0.49F;
    private static final float HEIGHT = 1.95F;

    // -- DataParameters --------------------------------------------------------

    /** True when Jenny is riding {@code SlimeEntity} (BEE) (index 118). */
    public static final EntityDataAccessor<Boolean> IS_BEE_RIDING =
            SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.BOOLEAN);

    // -- Fields ----------------------------------------------------------------

    /** True while Jenny is moving toward the bed for doggy-style. */
    public boolean movingToBed = false;

    /** True once Jenny reaches the bed (triggers sex start). */
    public boolean atBed = false;

    /** True when player's camera view has been set for paizuri. */
    public boolean paizuriCameraSet = false;

    /** Tick counter for bed-approach navigation. */
    private int bedApproachTicks = 0;

    /** Tick counter for sex-start position alignment. */
    private int alignTicks = 0;

    /** Whether Jenny is hard-thrusting (fast doggy variant toggle). */
    private boolean hardThrust = false;

    /** Attack animation counter (cycles 0-2). */
    private int attackVariant = 0;

    /** Bow-charge counter. */
    private int bowChargeCount = 0;

    /** GeckoLib4 cache. */
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // -- Factory ---------------------------------------------------------------

    public static JennyEntity createNaked(EntityType<? extends JennyEntity> type, Level level) {
        JennyEntity jenny = new JennyEntity(type, level);
        jenny.setNaked(true);
        return jenny;
    }

    // -- Constructor ----------------------------------------------------------

    public JennyEntity(EntityType<? extends JennyEntity> type, Level level) {
        super(type, level);
        this.dimensions = net.minecraft.world.entity.EntityDimensions.fixed(WIDTH, HEIGHT);
        // Offset values from original: P=140, O=50, K=140
        // V offset = (0, -0.03, -0.2)
        setSexPositionOffset(new Vec3(0.0D, -0.03D, -0.2D));
    }

    // -- Data -----------------------------------------------------------------

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IS_BEE_RIDING, false);
    }

    @Override
    public String getNpcName() { return "Jenny"; }

    @Override
    public float getEyeHeightOffset() { return -0.2F; }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose,
                              net.minecraft.world.entity.EntityDimensions dim) {
        return 1.64F;
    }

    // -- Sounds ---------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.randomFrom(ModSounds.GIRLS_JENNY_SIGH); }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return null; }

    // -- Tick -----------------------------------------------------------------

    @Override
    public void aiStep() {
        super.aiStep();

        // Sync BEE-riding state to clients
        if (!level().isClientSide()) {
            entityData.set(IS_BEE_RIDING, isPassenger() &&
                    getVehicle() instanceof SlimeNpcEntity);
        }

        if (movingToBed) tickBedApproach();
        if (atBed)       tickSexPositionAlign();
    }

    // -- Bed approach (doggy-style) --------------------------------------------

    /**
     * Navigates Jenny to the nearest available bed side for doggy-style.
     * Original {@code a()} on the server side.
     */
    private void findAndGotoBed() {
        BlockPos bedPos = findNearestBed(blockPosition());
        if (bedPos == null) {
            playSound(ModSounds.GIRLS_JENNY_HMPH[2]);
            displayDialogue("jenny.dialogue.nobedinsight");
            return;
        }

        Vec3 bedVec = Vec3.atLowerCornerOf(bedPos);
        int[][] offsets = {{0, 180, 0, -1}, {0, 0, 0, 1}, {-90, 0, -1, 0}, {90, 0, 1, 0}};
        int best = -1;

        for (int i = 0; i < offsets.length; i++) {
            Vec3 side = bedVec.add(offsets[i][2], 0, offsets[i][3]);
            BlockState sideState = level().getBlockState(new BlockPos((int)side.x, (int)side.y, (int)side.z));
            if (sideState.getBlock() == Blocks.AIR) {
                if (best == -1) {
                    best = i;
                } else {
                    double d1 = blockPosition().distSqr(new BlockPos(
                            (int)(bedVec.x + offsets[best][2]), (int)bedVec.y, (int)(bedVec.z + offsets[best][3])));
                    double d2 = blockPosition().distSqr(new BlockPos(
                            (int)(bedVec.x + offsets[i][2]),    (int)bedVec.y, (int)(bedVec.z + offsets[i][3])));
                    if (d2 < d1) best = i;
                }
            }
        }

        if (best == -1) {
            playSound(ModSounds.GIRLS_JENNY_HMPH[2]);
            displayDialogue("jenny.dialogue.bedobscured");
            return;
        }

        setFrozen(false);
        setTargetYaw(offsets[best][1]);
        Vec3 dest = bedVec.add(offsets[best][2], 0, offsets[best][3]);
        setTargetPosition(dest);
        setYRot((float) offsets[best][1]);
        getNavigation().stop();
        getNavigation().moveTo(dest.x, dest.y, dest.z, 0.35D);
        movingToBed = true;
        bedApproachTicks = 0;
    }

    private void tickBedApproach() {
        Vec3 target = getTargetPosition();
        if (target == null) { movingToBed = false; return; }

        if (position().distanceTo(target) < 0.6D || bedApproachTicks > 200) {
            // Reached bed
            movingToBed = false;
            bedApproachTicks = 0;
            entityData.set(FROZEN, true);
            getNavigation().stop();
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);

            if (entityData.get(IS_BEE_RIDING)) {
                callSexStartCallback();
            } else {
                setAnimState(AnimState.PAYMENT);
            }
        } else {
            bedApproachTicks++;
            if (bedApproachTicks % 60 == 0 || bedApproachTicks % 120 == 0) {
                getNavigation().stop();
                getNavigation().moveTo(target.x, target.y, target.z, 0.35D);
            }

            // Smooth rotation approach
            setYRot(getTargetYaw());
            Vec3 lerped = MathHelper.lerpVec(getTargetPosition(), getTargetPosition(),
                    40 - Math.min(40, bedApproachTicks));
            setPos(lerped.x, lerped.y, lerped.z);
        }
    }

    private void tickSexPositionAlign() {
        alignTicks++;
        Vec3 target = getTargetPosition();
        if (target == null || alignTicks > 40) {
            atBed = false;
            alignTicks = 0;
            setFrozen(false);
            setNoGravity(false);
            return;
        }
        Vec3 lerped = position().lerp(target, (double)(40 - alignTicks) / 40.0D);
        setPos(lerped.x, lerped.y, lerped.z);
    }

    // -- Interaction ----------------------------------------------------------

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (super.mobInteract(player, hand) == InteractionResult.SUCCESS)
            return InteractionResult.SUCCESS;

        if (level().isClientSide()) {
            if (!openMenu(player)) {
                displayDialogue("jenny.dialogue.busy");
            }
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Opens the action menu for the player.
     * Returns false if Jenny is busy with another player.
     */
    public boolean openMenu(Player player) {
        UUID ownerUUID = getOwnerUUID();

        // If Jenny has an owner that isn't this player, check if player can interact
        if (ownerUUID != null) {
            boolean isOwner = ownerUUID.equals(player.getUUID());
            if (!isOwner) return false;
        }

        boolean isNaked = entityData.get(MODEL_INDEX) == 1;
        String[] actions = {
                "action.names.blowjob",
                "action.names.boobjob",
                "action.names.doggy",
                isNaked ? "action.names.strip" : "action.names.dressup"
        };

        if (entityData.get(IS_BEE_RIDING)) {
            // Provide payment items
            openActionMenu(player, this, actions, true);
        } else {
            openActionMenu(player, this, actions, false);
        }
        return true;
    }

    // -- Action dispatch -------------------------------------------------------

    @Override
    public void onActionChosen(String actionKey, UUID playerUUID) {
        super.onActionChosen(actionKey, playerUUID);
        switch (actionKey) {
            case "action.names.blowjob"  -> { setAnimFollowUp("blowjob");       startSexApproach(true, playerUUID); }
            case "action.names.boobjob"  -> { setAnimFollowUp("boobjob");       startSexApproach(true, playerUUID); }
            case "action.names.doggy"    -> { setAnimFollowUp("doggy");         startSexApproach(true, playerUUID); }
            case "action.names.strip"    -> { setAnimFollowUp("strip");         startSexApproach(true, playerUUID); }
            case "action.names.dressup"  ->   setAnimState(AnimState.STRIP);
        }
    }

    /** Starts the sex approach animation (original {@code a(boolean, UUID)}). */
    @Override
    public void startSexApproach(boolean active, UUID playerUUID) {
        startSexApproach(active, true, playerUUID);
        ClientStateManager.setLeader(false);
    }

    // -- AnimState - sex state transitions ------------------------------------

    @Override
    protected AnimState getCumState(AnimState current) {
        return switch (current) {
            case SUCKBLOWJOB, THRUSTBLOWJOB -> {
                setOffsetPosition(0,0,0,0,70);
                yield AnimState.CUMBLOWJOB;
            }
            case DOGGYSLOW, DOGGYFAST -> AnimState.DOGGYCUM;
            case PAIZURI_FAST, PAIZURI_SLOW -> AnimState.PAIZURI_CUM;
            default -> null;
        };
    }

    @Override
    protected AnimState getFastVariant(AnimState current) {
        return switch (current) {
            case SUCKBLOWJOB -> AnimState.THRUSTBLOWJOB;
            case DOGGYSLOW   -> AnimState.DOGGYFAST;
            case PAIZURI_SLOW -> {
                if (paizuriCameraSet) { paizuriCameraSet = false; setOffsetPosition(0,0,0.2,0,70); }
                yield AnimState.PAIZURI_FAST;
            }
            default -> null;
        };
    }

    // -- setAnimState guard ----------------------------------------------------

    @Override
    public void setAnimState(AnimState newState) {
        AnimState cur = getAnimState();
        if (cur == AnimState.DOGGYCUM   && (newState == AnimState.DOGGYSLOW   || newState == AnimState.DOGGYFAST))   return;
        if (cur == AnimState.CUMBLOWJOB && (newState == AnimState.THRUSTBLOWJOB || newState == AnimState.SUCKBLOWJOB)) return;
        if (cur == AnimState.PAIZURI_CUM && (newState == AnimState.PAIZURI_SLOW || newState == AnimState.PAIZURI_FAST)) return;

        super.setAnimState(newState);

        // On blowjob/paizuri start: teleport player slightly behind
        if (cur == AnimState.STARTBLOWJOB || cur == AnimState.PAIZURI_START) {
            Player owner = getOwnerPlayer();
            if (owner == null) return;
            Vec3 behind = VectorMathUtil.rotateY(new Vec3(0,0,0.2), getTargetYaw() + 180F);
            owner.teleportTo(
                    owner.getX() + behind.x,
                    owner.getY(),
                    owner.getZ() + behind.z
            );
        }
    }

    // -- AI registration -------------------------------------------------------

    @Override
    protected void registerGoals() {
        super.registerGoals();
        addBedWanderGoal();
        goalSelector.addGoal(5, new com.trolmastercard.sexmod.entity.ai.ToggleableWatchGoal(
                this, Player.class, 3.0F, 1.0F));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.35D));
    }

    @Override
    public Vec3 getBonePosition(String boneName) {
        return null;
    }

    private void addBedWanderGoal() {
        // registered by addBedWanderGoal called from registerGoals
    }

    // -- animFollowUp action dispatch ------------------------------------------

    @Override
    protected void onAnimFollowUpTrigger(String followUp) {
        switch (followUp) {
            case "strip"    -> { rollClothing(); setAnimState(AnimState.STRIP); }
            case "blowjob"  ->   setAnimState(AnimState.STARTBLOWJOB);
            case "boobjob"  -> {
                if (entityData.get(MODEL_INDEX) != 0) { setAnimState(AnimState.STRIP); return; }
                setAnimState(AnimState.PAIZURI_START);
            }
            case "doggy"    -> {
                if (entityData.get(MODEL_INDEX) != 0) { setAnimState(AnimState.STRIP); rollClothing(); return; }
                onSessionReset();
                if (level().isClientSide()) {
                    ModNetwork.CHANNEL.sendToServer(new StartGalathSexPacket(getUUID()));
                }
                rollClothing();
                findAndGotoBed();
            }
        }
        setAnimFollowUp("");
    }

    // -- GeckoLib4 -------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Eyes
        registrar.add(new AnimationController<>(this, "eyes", 0, state -> {
            AnimState anim = getAnimState();
            if (anim == AnimState.NULL && anim.autoBlink) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.fhappy"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.null"));
        }));

        // Movement
        registrar.add(new AnimationController<>(this, "movement", 0, state -> {
            AnimState anim = getAnimState();
            if (anim != AnimState.NULL && anim != null) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.null"));
            }
            if (isPassenger()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.sit"));
            }
            double dx = getX() - xOld, dz = getZ() - zOld;
            if (Math.abs(dx) + Math.abs(dz) > 0.0D) {
                RawAnimation moveAnim = switch (getWalkAnimState()) {
                    case RUN      -> RawAnimation.begin().thenLoop("animation.jenny.run");
                    case FASTWALK -> RawAnimation.begin().thenLoop("animation.jenny.fastwalk");
                    default       -> RawAnimation.begin().thenLoop("animation.jenny.walk");
                };
                setYRot(yBodyRot);
                return state.setAndContinue(moveAnim);
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.idle"));
        }));

        // Action
        var actionCtrl = new AnimationController<>(this, "action", 0, this::handleActionState);
        actionCtrl.setSoundKeyframeHandler(this::handleSoundKeyframe);
        registrar.add(actionCtrl);
    }

    private PlayState handleActionState(AnimationState<JennyEntity> state) {
        RawAnimation raw = switch (getAnimState()) {
            case NULL           -> RawAnimation.begin().thenLoop("animation.jenny.null");
            case STRIP          -> RawAnimation.begin().thenPlay("animation.jenny.strip");
            case PAYMENT        -> RawAnimation.begin().thenPlay("animation.jenny.payment");
            case STARTBLOWJOB   -> RawAnimation.begin().thenPlay("animation.jenny.blowjobintro");
            case SUCKBLOWJOB    -> RawAnimation.begin().thenLoop("animation.jenny.blowjobsuck");
            case THRUSTBLOWJOB  -> RawAnimation.begin().thenLoop("animation.jenny.blowjobthrust");
            case CUMBLOWJOB     -> RawAnimation.begin().thenPlay("animation.jenny.blowjobcum");
            case DOGGYSTART     -> RawAnimation.begin().thenPlay("animation.jenny.doggygoonbed");
            case WAITDOGGY      -> RawAnimation.begin().thenLoop("animation.jenny.doggywait");
            case STARTDOGGY     -> RawAnimation.begin().thenPlay("animation.jenny.doggystart");
            case DOGGYSLOW      -> RawAnimation.begin().thenLoop("animation.jenny.doggyslow");
            case DOGGYFAST      -> RawAnimation.begin().thenLoop("animation.jenny.doggyfast_" + (hardThrust ? "hard" : "soft"));
            case DOGGYCUM       -> RawAnimation.begin().thenPlay("animation.jenny.doggycum");
            case ATTACK         -> RawAnimation.begin().thenPlay("animation.jenny.attack" + attackVariant);
            case BOWCHARGE      -> RawAnimation.begin().thenPlay("animation.jenny.bowcharge");
            case RIDE           -> RawAnimation.begin().thenLoop("animation.jenny.ride");
            case SIT            -> RawAnimation.begin().thenLoop("animation.jenny.sit");
            case THROWPEARL     -> RawAnimation.begin().thenPlay("animation.jenny.throwpearl");
            case DOWNED         -> RawAnimation.begin().thenLoop("animation.jenny.downed");
            case PAIZURI_START  -> RawAnimation.begin().thenPlay("animation.jenny.paizuri_start");
            case PAIZURI_SLOW   -> RawAnimation.begin().thenLoop("animation.jenny.paizuri_slow");
            case PAIZURI_FAST   -> RawAnimation.begin().thenLoop("animation.jenny.paizuri_fast");
            case PAIZURI_CUM    -> RawAnimation.begin().thenPlay("animation.jenny.paizuri_cum");
            case WAVE           -> RawAnimation.begin().thenLoop("animation.jenny.wave");
            case WAVE_IDLE      -> RawAnimation.begin().thenLoop("animation.jenny.wave_idle");
            default             -> RawAnimation.begin().thenLoop("animation.jenny.null");
        };
        return state.setAndContinue(raw);
    }

    // -- Sound keyframes -------------------------------------------------------

    @OnlyIn(Dist.CLIENT)
    // 🚨 CORREGIDO: Ruta actualizada a keyframe.event 🚨
    private void handleSoundKeyframe(
            software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent<JennyEntity> e) {
        String s = e.getKeyframeData().getSound();
        switch (s) {
            // -- Attack ------------------------------------------------------
            case "attackSound" -> playSound(SoundEvents.PLAYER_ATTACK_SWEEP);
            case "attackDone"  -> { setAnimState(AnimState.NULL); if (++attackVariant == 3) attackVariant = 0; }
            // -- Strip / dress ------------------------------------------------
            case "becomeNude"  -> { if (isNakedToggle()) toggleModelIndex(); }
            case "stripDone"   -> { if (!"boobjob".equals(getAnimFollowUp())) onSessionReset(); onAnimFollowUpTrigger(getAnimFollowUp()); }
            case "stripMSG1"   -> { displaySubtitle("jenny.dialogue.hihi"); playRandSound(ModSounds.GIRLS_JENNY_GIGGLE); }
            // -- Payment ------------------------------------------------------
            case "paymentMSG1" -> { displaySubtitle("jenny.dialogue.huh"); playSound(ModSounds.GIRLS_JENNY_HUH[1]); }
            case "paymentMSG2" -> { playSound(ModSounds.MISC_PLOB[0], 0.5F); sendChatProposition(); }
            case "paymentMSG3" -> { displaySubtitle("jenny.dialogue.hehe"); playRandSound(ModSounds.GIRLS_JENNY_GIGGLE); }
            case "paymentMSG4" -> playSound(ModSounds.MISC_PLOB[0], 0.25F);
            case "paymentDone" -> onAnimFollowUpTrigger(getAnimFollowUp());
            // -- Blowjob intro ------------------------------------------------
            case "bjiMSG1"  -> { displaySubtitle("jenny.dialogue.blowjobtext1"); playSound(ModSounds.GIRLS_JENNY_MMM[8]); setYRot(getYRot() + 180F); if (isOwnerLocal()) HornyMeterOverlay.onSexStart(); }
            case "bjiMSG2"  -> { displaySubtitle("jenny.dialogue.blowjobtext2"); playSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING[8]); }
            case "bjiMSG3"  -> { displaySubtitle("jenny.dialogue.blowjobtext3"); playSound(ModSounds.GIRLS_JENNY_AFTERSESSIONMOAN[0]); }
            case "bjiMSG4"  ->   playSound(ModSounds.MISC_BELLJINGLE[0]);
            case "bjiMSG5"  -> { displaySubtitle("jenny.dialogue.blowjobtext4"); playSound(ModSounds.GIRLS_JENNY_HMPH[1], 0.5F); if (isOwnerLocal()) HornyMeterOverlay.onSexStart(); }
            case "bjiMSG6"  -> { displaySubtitle("jenny.dialogue.blowjobtext5"); playSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING[8]); }
            case "bjiMSG7"  -> { displaySubtitle("jenny.dialogue.blowjobtext6"); playSound(ModSounds.GIRLS_JENNY_GIGGLE[4]); }
            case "bjiMSG8"  -> { displayChatMessage("jenny.dialogue.blowjobtext7"); playSound(ModSounds.MISC_PLOB[0], 0.5F); }
            case "bjiMSG9"  -> { displaySubtitle("jenny.dialogue.blowjobtext8"); playSound(ModSounds.GIRLS_JENNY_GIGGLE[2]); }
            case "bjiMSG10" -> { if (isOwnerLocal()) setOwnerCamera(-0.65D, -0.8D, -0.25D, 60F, -3F); }
            case "bjiMSG11" -> { if (isOwnerLocal() && ClientStateManager.isLeader()) onFastRoundComplete(); playRandSound(ModSounds.GIRLS_JENNY_LIPSOUND); if (isOwnerLocal()) HornyMeterOverlay.addValue(0.02D); }
            case "bjiMSG12" -> { if (getRandom().nextInt(5) == 0) playRandSound(ModSounds.GIRLS_JENNY_BJMOAN); playRandSound(ModSounds.GIRLS_JENNY_LIPSOUND); if (isOwnerLocal()) HornyMeterOverlay.addValue(0.02D); }
            case "bjtMSG1"  -> { playRandSound(ModSounds.GIRLS_JENNY_MMM); playRandSound(ModSounds.GIRLS_JENNY_LIPSOUND); if (isOwnerLocal()) HornyMeterOverlay.addValue(0.04D); }
            case "bjiDone"  -> { setAnimState(AnimState.SUCKBLOWJOB); if (isOwnerLocal()) HornyMeterOverlay.setVisible(false); }
            case "bjtDone"  ->   setAnimState(AnimState.SUCKBLOWJOB);
            case "sexUiOn"  -> { if (isOwnerLocal()) HornyMeterOverlay.setVisible(true); }
            // -- Fast blowjob ready -------------------------------------------
            case "bjtReady", "paizuriReady", "doggyfastReady" -> {
                if (isOwnerLocal() && ClientStateManager.isLeader()) onFastRoundComplete();
            }
            // -- Blowjob cum --------------------------------------------------
            case "bjcMSG1" -> playSound(ModSounds.GIRLS_JENNY_BJMOAN[1]);
            case "bjcMSG2" -> { playSound(ModSounds.GIRLS_JENNY_BJMOAN[7]); if (isOwnerLocal()) HornyMeterOverlay.stop(); }
            case "bjcMSG3" -> playSound(ModSounds.GIRLS_JENNY_AFTERSESSIONMOAN[1]);
            case "bjcMSG4" -> playSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING[0]);
            case "bjcMSG5" -> playSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING[1]);
            case "bjcMSG6" -> playSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING[2]);
            case "bjcMSG7" -> playSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING[3]);
            case "bjcBlackScreen" -> { if (isOwnerLocal()) ClientStateManager.triggerBlackScreen(); }
            case "bjcDone", "paizuri_cumDone", "doggyCumDone" -> {
                if (isOwnerLocal()) { HornyMeterOverlay.onSexEnd(); onSessionReset(); }
            }
            // -- Doggy on bed -------------------------------------------------
            case "doggyGoOnBedMSG1" -> { playSound(ModSounds.MISC_BEDRUSTLE[0]); setYRot(yBodyRot); }
            case "doggyGoOnBedMSG2" -> { displayDialogue("jenny.dialogue.doggytext1"); playSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING[9]); }
            case "doggyGoOnBedMSG3" -> { displayDialogue("jenny.dialogue.doggytext2"); playRandSound(ModSounds.GIRLS_JENNY_GIGGLE); }
            case "doggyGoOnBedMSG4" ->   playSound(ModSounds.MISC_SLAP[0], 0.75F);
            case "doggyGoOnBedDone" -> {
                ModNetwork.CHANNEL.sendToServer(
                        new SetPlayerForNpcPacket(getUUID(), net.minecraft.client.Minecraft.getInstance().player.getUUID()));
                setAnimState(AnimState.WAITDOGGY);
            }
            // -- Doggy start --------------------------------------------------
            case "doggystartMSG1" ->   playSound(ModSounds.MISC_TOUCH[0]);
            case "doggystartMSG2" ->   playSound(ModSounds.MISC_TOUCH[1]);
            case "doggystartMSG3" ->   playSound(ModSounds.MISC_BEDRUSTLE[1], 0.5F);
            case "doggystartMSG4" -> { playRandSound(ModSounds.MISC_SMALLINSERTS); playSound(ModSounds.GIRLS_JENNY_MMM[1]); if (isOwnerLocal()) HornyMeterOverlay.onSexStart(); }
            case "doggystartMSG5" -> { playRandSound(ModSounds.MISC_POUNDING, 0.33F); playRandSound(ModSounds.GIRLS_JENNY_MOAN); }
            case "doggystartDone" -> { setAnimState(AnimState.DOGGYSLOW); if (isOwnerLocal()) HornyMeterOverlay.setVisible(false); }
            // -- Doggy slow ---------------------------------------------------
            case "doggyslowMSG1" -> {
                hardThrust = false;
                playRandSound(ModSounds.MISC_POUNDING, 0.33F);
                int r = getRandom().nextInt(4);
                if (r == 0) { if (getRandom().nextBoolean()) playRandSound(ModSounds.GIRLS_JENNY_MMM); else playRandSound(ModSounds.GIRLS_JENNY_MOAN); }
                else playRandSound(ModSounds.GIRLS_JENNY_HEAVYBREATHING);
                if (isOwnerLocal()) HornyMeterOverlay.addValue(0.00666D);
            }
            case "doggyslowMSG2" ->   playRandSound(ModSounds.GIRLS_JENNY_LIGHTBREATHING, 0.5F);
            // -- Doggy fast ---------------------------------------------------
            case "doggyfastMSG1" -> {
                playRandSound(ModSounds.MISC_POUNDING, 0.75F);
                if (isOwnerLocal()) HornyMeterOverlay.addValue(0.02D);
                if (bowChargeCount++ % 2 == 0) {
                    if (getRandom().nextBoolean()) playRandSound(ModSounds.GIRLS_JENNY_MOAN);
                    else playRandSound(ModSounds.GIRLS_JENNY_HEAVYBREATHING);
                } else {
                    playRandSound(ModSounds.GIRLS_JENNY_AHH);
                }
            }
            case "doggyfastDone" -> { hardThrust = false; setAnimState(AnimState.DOGGYSLOW); }
            // -- Doggy cum ----------------------------------------------------
            case "doggycumMSG1" -> { playSound(ModSounds.MISC_CUMINFLATION[0], 2F); playRandSound(ModSounds.MISC_POUNDING, 2F); playRandSound(ModSounds.GIRLS_JENNY_MOAN); }
            case "doggycumMSG2" -> playSound(ModSounds.GIRLS_JENNY_HEAVYBREATHING[4]);
            case "doggycumMSG3" -> playSound(ModSounds.GIRLS_JENNY_HEAVYBREATHING[5]);
            case "doggycumMSG4" -> playSound(ModSounds.GIRLS_JENNY_HEAVYBREATHING[6]);
            case "doggycumMSG5" -> playSound(ModSounds.GIRLS_JENNY_HEAVYBREATHING[7]);
            // -- Pearl throw --------------------------------------------------
            case "pearl" -> ModNetwork.CHANNEL.sendToServer(
                    new com.trolmastercard.sexmod.network.packet.SpawnEnergyBallParticlesPacket(getUUID()));
            // -- Paizuri ------------------------------------------------------
            case "boobjob_camera" -> {
                UUID uid = net.minecraft.client.Minecraft.getInstance().player.getUUID();
                Player nearPlayer = level().getPlayerByUUID(uid);
                if (nearPlayer != null) {
                    setYRot(nearPlayer.getYRot());
                    setOwnerCamera(uid);
                    if (!paizuriCameraSet) { paizuriCameraSet = true; setOwnerCamera(-0.7D, -0.6D, 0.2D, 60F, -3F); }
                }
            }
            case "paizuri_startDone" -> { if (isOwnerLocal()) { setAnimState(AnimState.PAIZURI_SLOW); HornyMeterOverlay.onSexStart(); HornyMeterOverlay.setVisible(false); } }
            case "paizuriFastMSG1"   -> { playRandSound(ModSounds.MISC_POUNDING); if (getRandom().nextBoolean()) playRandSound(ModSounds.GIRLS_JENNY_MMM); else playRandSound(ModSounds.GIRLS_JENNY_AHH); if (isOwnerLocal()) HornyMeterOverlay.addValue(0.04D); }
            case "paizuriSlowMSG1", "paizuriStartMSG1" -> { playRandSound(ModSounds.MISC_POUNDING); if (isOwnerLocal()) HornyMeterOverlay.addValue(0.02D); }
            case "paizuri_fastDone"  -> {
                setAnimState(AnimState.PAIZURI_SLOW);
                if (isOwnerLocal() && !paizuriCameraSet) { paizuriCameraSet = true; setOwnerCamera(-0.7D, -0.6D, 0.2D, 60F, -3F); }
            }
            case "paizuri_startStep" -> {
                BlockState bs = level().getBlockState(blockPosition().above());
                playSound(bs.getSoundType(level(), blockPosition(), this).getStepSound());
            }
            case "paizuri_cumStart"  -> {
                if (isOwnerLocal() && !paizuriCameraSet) { setOwnerCamera(-0.7D, -0.6D, 0.2D, 60F, -3F); }
            }
        }
    }

    // -- Helper: send chat proposition ----------------------------------------

    @OnlyIn(Dist.CLIENT)
    private void sendChatProposition() {
        String player = net.minecraft.client.Minecraft.getInstance().player.getName().getString();
        String followUp = getAnimFollowUp();
        String key = switch (followUp) {
            case "strip"    -> "jenny.dialogue.showBobsandveganapls";
            case "blowjob"  -> "jenny.dialogue.giveblowjob";
            case "doggy"    -> "jenny.dialogue.givesex";
            case "boobjob"  -> "jenny.dialogue.givebooba";
            default         -> "jenny.dialogue.givesex";
        };
        displayChatMessageRaw("<" + player + "> " + net.minecraft.client.resources.language.I18n.get(key));
        displayChatMessageRaw("<" + player + "> sex pls");
    }

    // -- GeckoLib4 boilerplate -------------------------------------------------

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    // -- GalathSexEntity interface ---------------------------------------------

    @Override
    public void onGalathSexStart() { atBed = true; }

    // -- TickableCallback ------------------------------------------------------

    @Override
    public void onTick() { /* handled in aiStep */ }

    // -- Private util ----------------------------------------------------------

    private BlockPos findNearestBed(BlockPos origin) {
        int r = 8;
        for (int dx = -r; dx <= r; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -r; dz <= r; dz++) {
            BlockPos pos = origin.offset(dx, dy, dz);
            if (level().getBlockState(pos).getBlock() instanceof BedBlock) return pos;
        }
        return null;
    }

    private boolean isNakedToggle() {
        return entityData.get(MODEL_INDEX) == 1;
    }

    private void toggleModelIndex() {
        int cur = entityData.get(MODEL_INDEX);
        entityData.set(MODEL_INDEX, cur == 1 ? 0 : 1);
    }
}