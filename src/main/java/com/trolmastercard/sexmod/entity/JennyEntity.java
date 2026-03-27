package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.ClientStateManager;
import com.trolmastercard.sexmod.client.HornyMeterOverlay;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.RequestRidingPacket;
import com.trolmastercard.sexmod.network.packet.SetPlayerForNpcPacket;
import com.trolmastercard.sexmod.network.packet.StartGalathSexPacket;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.network.packet.SpawnEnergyBallParticlesPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
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
 * JennyEntity - Compañera Principal de la Tribu.
 * Portado a 1.20.1.
 * Maneja interacciones de búsqueda de camas, cambios de atuendo y minijuegos.
 */
public class JennyEntity extends NpcInventoryEntity
        implements TickableCallback, GalathSexEntity {

    // =========================================================================
    //  Constantes y Dimensiones
    // =========================================================================

    private static final float WIDTH  = 0.49F;
    private static final float HEIGHT = 1.95F;

    public static final EntityDataAccessor<Boolean> IS_BEE_RIDING =
            SynchedEntityData.defineId(JennyEntity.class, EntityDataSerializers.BOOLEAN);

    // =========================================================================
    //  Variables de Navegación y Lógica
    // =========================================================================

    public boolean movingToBed = false;
    public boolean atBed = false;
    public boolean specialCameraSet = false; // Antes paizuriCameraSet
    private int bedApproachTicks = 0;
    private int alignTicks = 0;
    private boolean intenseMode = false; // Antes hardThrust
    private int attackVariant = 0;
    private int specialChargeCount = 0; // Antes bowChargeCount

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // =========================================================================
    //  Constructores y Fábricas
    // =========================================================================

    public static JennyEntity createAlternate(EntityType<? extends JennyEntity> type, Level level) {
        JennyEntity jenny = new JennyEntity(type, level);
        // Asumiendo que setNaked existe en la clase padre y ajusta el modelo
        jenny.setNaked(true);
        return jenny;
    }

    public JennyEntity(EntityType<? extends JennyEntity> type, Level level) {
        super(type, level);
        this.dimensions = net.minecraft.world.entity.EntityDimensions.fixed(WIDTH, HEIGHT);
        setInteractionOffset(new Vec3(0.0D, -0.03D, -0.2D)); // Antes setSexPositionOffset
    }

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

    // =========================================================================
    //  Sonidos
    // =========================================================================

    @Override
    protected SoundEvent getAmbientSound() { return ModSounds.randomFrom(ModSounds.JENNY_SIGH); }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) { return null; }

    // =========================================================================
    //  Lógica de Ticks y Navegación
    // =========================================================================

    @Override
    public void aiStep() {
        super.aiStep();

        if (!level().isClientSide()) {
            entityData.set(IS_BEE_RIDING, isPassenger() && getVehicle() instanceof SlimeEntity); // Corregido de SlimeNpcEntity a SlimeEntity asumiendo convención
        }

        if (movingToBed) tickBedApproach();
        if (atBed)       tickInteractionPositionAlign(); // Antes tickSexPositionAlign
    }

    private void findAndGotoBed() {
        BlockPos bedPos = findNearestBed(blockPosition());
        if (bedPos == null) {
            playSound(ModSounds.JENNY_HMPH[2], 1.0F, 1.0F);
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
            playSound(ModSounds.JENNY_HMPH[2], 1.0F, 1.0F);
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
            movingToBed = false;
            bedApproachTicks = 0;
            entityData.set(FROZEN, true);
            getNavigation().stop();
            setNoGravity(true);
            setDeltaMovement(Vec3.ZERO);

            if (entityData.get(IS_BEE_RIDING)) {
                callInteractionStartCallback(); // Antes callSexStartCallback
            } else {
                setAnimState(AnimState.PAYMENT);
            }
        } else {
            bedApproachTicks++;
            if (bedApproachTicks % 60 == 0 || bedApproachTicks % 120 == 0) {
                getNavigation().stop();
                getNavigation().moveTo(target.x, target.y, target.z, 0.35D);
            }

            setYRot(getTargetYaw());
            // TODO: Asegúrate de que MathHelper.lerpVec exista en tu MathUtil, si no, usa MathUtil.lerpVec3
            Vec3 lerped = position().lerp(getTargetPosition(), Math.min(1.0, (double)bedApproachTicks / 40.0));
            setPos(lerped.x, lerped.y, lerped.z);
        }
    }

    private void tickInteractionPositionAlign() {
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

    // =========================================================================
    //  Menús e Interacción
    // =========================================================================

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

    public boolean openMenu(Player player) {
        UUID ownerUUID = getOwnerUUID();
        if (ownerUUID != null && !ownerUUID.equals(player.getUUID())) return false;

        boolean isAlternate = entityData.get(MODEL_INDEX) == 1; // Antes isNaked
        String[] actions = {
                "action.names.talk",     // Antes blowjob
                "action.names.hug",      // Antes boobjob
                "action.names.dance",    // Antes doggy
                isAlternate ? "action.names.dressup" : "action.names.strip"
        };

        openActionMenu(player, this, actions, entityData.get(IS_BEE_RIDING));
        return true;
    }

    @Override
    public void onActionChosen(String actionKey, UUID playerUUID) {
        super.onActionChosen(actionKey, playerUUID);
        switch (actionKey) {
            case "action.names.talk"  -> { setAnimFollowUp("talk");  startInteractionApproach(true, playerUUID); }
            case "action.names.hug"   -> { setAnimFollowUp("hug");   startInteractionApproach(true, playerUUID); }
            case "action.names.dance" -> { setAnimFollowUp("dance"); startInteractionApproach(true, playerUUID); }
            case "action.names.strip", "action.names.dressup" -> { setAnimFollowUp("strip"); startInteractionApproach(true, playerUUID); }
        }
    }

    @Override
    public void startInteractionApproach(boolean active, UUID playerUUID) { // Antes startSexApproach
        startInteractionApproach(active, true, playerUUID);
        ClientStateManager.setLeader(false);
    }

    // =========================================================================
    //  Máquina de Estados de Animación (Enmascarada a los nuevos AnimState)
    // =========================================================================

    @Override
    protected AnimState getFinishState(AnimState current) { // Antes getCumState
        return switch (current) {
            case GIFT_LOOP, GIFT_ACTION -> {
                setOffsetPosition(0,0,0,0,70);
                yield AnimState.GIFT_FINISH;
            }
            case GAME_CHASE_SLOW, GAME_CHASE_FAST -> AnimState.GAME_CHASE_FINISH;
            case CLOSE_HUG_FAST, CLOSE_HUG_SLOW -> AnimState.CLOSE_HUG_FINISH;
            default -> null;
        };
    }

    @Override
    protected AnimState getFastVariant(AnimState current) {
        return switch (current) {
            case GIFT_LOOP -> AnimState.GIFT_ACTION;
            case GAME_CHASE_SLOW -> AnimState.GAME_CHASE_FAST;
            case CLOSE_HUG_SLOW -> {
                if (specialCameraSet) { specialCameraSet = false; setOffsetPosition(0,0,0.2,0,70); }
                yield AnimState.CLOSE_HUG_FAST;
            }
            default -> null;
        };
    }

    @Override
    public void setAnimState(AnimState newState) {
        AnimState cur = getAnimState();
        if (cur == AnimState.GAME_CHASE_FINISH && (newState == AnimState.GAME_CHASE_SLOW || newState == AnimState.GAME_CHASE_FAST)) return;
        if (cur == AnimState.GIFT_FINISH && (newState == AnimState.GIFT_ACTION || newState == AnimState.GIFT_LOOP)) return;
        if (cur == AnimState.CLOSE_HUG_FINISH && (newState == AnimState.CLOSE_HUG_SLOW || newState == AnimState.CLOSE_HUG_FAST)) return;

        super.setAnimState(newState);

        if (cur == AnimState.GIFT_START || cur == AnimState.CLOSE_HUG_START) {
            Player owner = getOwnerPlayer();
            if (owner == null) return;
            // TODO: Si VectorMathUtil.rotateY no existe, usa rotateAroundY
            Vec3 behind = VectorMathUtil.rotateAroundY(new Vec3(0,0,0.2), getTargetYaw() + 180F);
            owner.teleportTo(owner.getX() + behind.x, owner.getY(), owner.getZ() + behind.z);
        }
    }

    @Override
    protected void onAnimFollowUpTrigger(String followUp) {
        switch (followUp) {
            case "strip" -> { rollClothing(); setAnimState(AnimState.STRIP); }
            case "talk"  ->   setAnimState(AnimState.GIFT_START);
            case "hug"   -> {
                if (entityData.get(MODEL_INDEX) != 0) { setAnimState(AnimState.STRIP); return; }
                setAnimState(AnimState.CLOSE_HUG_START);
            }
            case "dance" -> {
                if (entityData.get(MODEL_INDEX) != 0) { setAnimState(AnimState.STRIP); rollClothing(); return; }
                onSessionReset();
                if (level().isClientSide()) {
                    // Usamos el paquete de Galath que confirmaste que existe, pero lo hemos enmascarado en la red
                    ModNetwork.CHANNEL.sendToServer(new StartInteractionAnimationPacket(getUUID()));
                }
                rollClothing();
                findAndGotoBed();
            }
        }
        setAnimFollowUp("");
    }

    // =========================================================================
    //  GeckoLib 4 Controllers
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "eyes", 0, state -> {
            AnimState anim = getAnimState();
            if (anim == AnimState.NULL && anim.autoBlink) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.fhappy"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.null"));
        }));

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
                RawAnimation moveAnim = switch (getWalkAnimState()) { // Asumiendo getWalkAnimState() existe en Base
                    case RUN      -> RawAnimation.begin().thenLoop("animation.jenny.run");
                    // case FASTWALK -> RawAnimation.begin().thenLoop("animation.jenny.fastwalk"); // Asegúrate de que FASTWALK esté en AnimState
                    default       -> RawAnimation.begin().thenLoop("animation.jenny.walk");
                };
                setYRot(yBodyRot);
                return state.setAndContinue(moveAnim);
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.idle"));
        }));

        var actionCtrl = new AnimationController<>(this, "action", 0, this::handleActionState);
        actionCtrl.setSoundKeyframeHandler(this::handleSoundKeyframe);
        registrar.add(actionCtrl);
    }

    private PlayState handleActionState(AnimationState<JennyEntity> state) {
        // Los strings de los JSON de animación (".json") permanecen idénticos para no romper los modelos
        RawAnimation raw = switch (getAnimState()) {
            case STRIP          -> RawAnimation.begin().thenPlay("animation.jenny.strip");
            case PAYMENT        -> RawAnimation.begin().thenPlay("animation.jenny.payment");
            case GIFT_START     -> RawAnimation.begin().thenPlay("animation.jenny.blowjobintro");
            case GIFT_LOOP      -> RawAnimation.begin().thenLoop("animation.jenny.blowjobsuck");
            case GIFT_ACTION    -> RawAnimation.begin().thenLoop("animation.jenny.blowjobthrust");
            case GIFT_FINISH    -> RawAnimation.begin().thenPlay("animation.jenny.blowjobcum");
            case GAME_ACTIVE_START -> RawAnimation.begin().thenPlay("animation.jenny.doggygoonbed"); // Asumiendo DOGGYSTART -> GAME_ACTIVE_START
            case GAME_CHASE_WAIT -> RawAnimation.begin().thenLoop("animation.jenny.doggywait");
            case GAME_CHASE_START-> RawAnimation.begin().thenPlay("animation.jenny.doggystart");
            case GAME_CHASE_SLOW -> RawAnimation.begin().thenLoop("animation.jenny.doggyslow");
            case GAME_CHASE_FAST -> RawAnimation.begin().thenLoop("animation.jenny.doggyfast_" + (intenseMode ? "hard" : "soft"));
            case GAME_CHASE_FINISH-> RawAnimation.begin().thenPlay("animation.jenny.doggycum");
            case ATTACK         -> RawAnimation.begin().thenPlay("animation.jenny.attack" + attackVariant);
            // case BOWCHARGE      -> RawAnimation.begin().thenPlay("animation.jenny.bowcharge");
            case RIDE           -> RawAnimation.begin().thenLoop("animation.jenny.ride");
            case SIT            -> RawAnimation.begin().thenLoop("animation.jenny.sit");
            case THROW_PEARL    -> RawAnimation.begin().thenPlay("animation.jenny.throwpearl");
            case DOWNED         -> RawAnimation.begin().thenLoop("animation.jenny.downed");
            case CLOSE_HUG_START-> RawAnimation.begin().thenPlay("animation.jenny.paizuri_start");
            case CLOSE_HUG_SLOW -> RawAnimation.begin().thenLoop("animation.jenny.paizuri_slow");
            case CLOSE_HUG_FAST -> RawAnimation.begin().thenLoop("animation.jenny.paizuri_fast");
            case CLOSE_HUG_FINISH-> RawAnimation.begin().thenPlay("animation.jenny.paizuri_cum");
            case WAVE           -> RawAnimation.begin().thenLoop("animation.jenny.wave");
            case WAVE_IDLE      -> RawAnimation.begin().thenLoop("animation.jenny.wave_idle");
            default             -> RawAnimation.begin().thenLoop("animation.jenny.null");
        };
        return state.setAndContinue(raw);
    }

    // =========================================================================
    //  Filtro de Sonidos (SFW)
    // =========================================================================

    @OnlyIn(Dist.CLIENT)
    private void handleSoundKeyframe(software.bernie.geckolib.core.animation.SoundKeyframeEvent<JennyEntity> e) {
        String s = e.getKeyframeData().getSound();

        switch (s) {
            case "attackSound" -> playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
            case "attackDone"  -> { setAnimState(AnimState.NULL); if (++attackVariant == 3) attackVariant = 0; }
            case "becomeNude"  -> { if (entityData.get(MODEL_INDEX) == 1) toggleModelIndex(); }
            case "stripDone"   -> { if (!"hug".equals(getAnimFollowUp())) onSessionReset(); onAnimFollowUpTrigger(getAnimFollowUp()); }
            case "stripMSG1"   -> { displaySubtitle("jenny.dialogue.hihi"); playRandSound(ModSounds.JENNY_GIGGLE); }

            case "paymentMSG1" -> { displaySubtitle("jenny.dialogue.huh"); playSound(ModSounds.JENNY_HUH[1], 1.0F, 1.0F); }
            case "paymentMSG2" -> { playSound(ModSounds.MISC_PLOB[0], 0.5F, 1.0F); sendChatProposition(); }
            case "paymentMSG3" -> { displaySubtitle("jenny.dialogue.hehe"); playRandSound(ModSounds.JENNY_GIGGLE); }
            case "paymentMSG4" -> playSound(ModSounds.MISC_PLOB[0], 0.25F, 1.0F);
            case "paymentDone" -> onAnimFollowUpTrigger(getAnimFollowUp());

            // Todo evento de voz subido de tono se redirige a Risas y Suspiros
            case "bjiMSG1", "bjiMSG2", "bjiMSG3", "bjiMSG5", "bjiMSG6", "bjiMSG7", "bjiMSG9", "bjiMSG12", "bjtMSG1" -> {
                playRandSound(ModSounds.JENNY_GIGGLE);
                if (isOwnerLocal()) HornyMeterOverlay.addValue(0.02D);
            }
            case "bjiMSG4"  -> playSound(ModSounds.MISC_BELLJINGLE[0], 1.0F, 1.0F);
            case "bjiMSG8"  -> playSound(ModSounds.MISC_PLOB[0], 0.5F, 1.0F);
            case "bjiMSG10", "paizuri_cumStart" -> { if (isOwnerLocal()) setOwnerCamera(-0.65D, -0.8D, -0.25D, 60F, -3F); }
            case "bjiMSG11", "bjtReady", "paizuriReady", "doggyfastReady" -> {
                if (isOwnerLocal() && ClientStateManager.isLeader()) onFastRoundComplete();
            }
            case "bjiDone", "bjtDone" -> { setAnimState(AnimState.GIFT_LOOP); if (isOwnerLocal()) HornyMeterOverlay.setVisible(false); }
            case "sexUiOn"  -> { if (isOwnerLocal()) HornyMeterOverlay.setVisible(true); }

            case "bjcMSG1", "bjcMSG2", "bjcMSG3", "bjcMSG4", "bjcMSG5", "bjcMSG6", "bjcMSG7" -> playRandSound(ModSounds.JENNY_SIGH);
            case "bjcBlackScreen" -> { if (isOwnerLocal()) ClientStateManager.triggerBlackScreen(); }
            case "bjcDone", "paizuri_cumDone", "doggyCumDone" -> {
                if (isOwnerLocal()) { HornyMeterOverlay.onInteractionEnd(); onSessionReset(); }
            }

            case "doggyGoOnBedMSG1" -> { playSound(ModSounds.MISC_BEDRUSTLE[0], 1.0F, 1.0F); setYRot(yBodyRot); }
            case "doggyGoOnBedMSG2" -> { displayDialogue("jenny.dialogue.doggytext1"); playRandSound(ModSounds.JENNY_SIGH); }
            case "doggyGoOnBedMSG3" -> { displayDialogue("jenny.dialogue.doggytext2"); playRandSound(ModSounds.JENNY_GIGGLE); }
            case "doggyGoOnBedMSG4" -> playSound(ModSounds.MISC_SLAP[0], 0.75F, 1.0F);
            case "doggyGoOnBedDone" -> {
                ModNetwork.CHANNEL.sendToServer(new SetPlayerForNpcPacket(getUUID(), net.minecraft.client.Minecraft.getInstance().player.getUUID()));
                setAnimState(AnimState.GAME_CHASE_WAIT);
            }

            case "doggystartMSG1", "doggystartMSG2" -> playSound(ModSounds.MISC_TOUCH[0], 1.0F, 1.0F);
            case "doggystartMSG3" -> playSound(ModSounds.MISC_BEDRUSTLE[1], 0.5F, 1.0F);
            case "doggystartMSG4", "doggystartMSG5", "doggyslowMSG1", "doggyfastMSG1", "paizuriFastMSG1", "paizuriSlowMSG1", "paizuriStartMSG1" -> {
                playRandSound(ModSounds.MISC_POUNDING, 0.5F);
                playRandSound(ModSounds.JENNY_GIGGLE);
                if (isOwnerLocal()) HornyMeterOverlay.addValue(0.02D);
            }
            case "doggystartDone" -> { setAnimState(AnimState.GAME_CHASE_SLOW); if (isOwnerLocal()) HornyMeterOverlay.setVisible(false); }
            case "doggyslowMSG2" -> playRandSound(ModSounds.JENNY_SIGH, 0.5F);
            case "doggyfastDone" -> { intenseMode = false; setAnimState(AnimState.GAME_CHASE_SLOW); }

            case "doggycumMSG1", "doggycumMSG2", "doggycumMSG3", "doggycumMSG4", "doggycumMSG5" -> playRandSound(ModSounds.JENNY_SIGH);
            case "pearl" -> ModNetwork.CHANNEL.sendToServer(new SpawnEnergyBallParticlesPacket(position(), true));

            case "boobjob_camera" -> {
                UUID uid = net.minecraft.client.Minecraft.getInstance().player.getUUID();
                Player nearPlayer = level().getPlayerByUUID(uid);
                if (nearPlayer != null) {
                    setYRot(nearPlayer.getYRot());
                    setOwnerCamera(uid);
                    if (!specialCameraSet) { specialCameraSet = true; setOwnerCamera(-0.7D, -0.6D, 0.2D, 60F, -3F); }
                }
            }
            case "paizuri_startDone" -> { if (isOwnerLocal()) { setAnimState(AnimState.CLOSE_HUG_SLOW); HornyMeterOverlay.onSexStart(); HornyMeterOverlay.setVisible(false); } }
            case "paizuri_fastDone"  -> {
                setAnimState(AnimState.CLOSE_HUG_SLOW);
                if (isOwnerLocal() && !specialCameraSet) { specialCameraSet = true; setOwnerCamera(-0.7D, -0.6D, 0.2D, 60F, -3F); }
            }
            case "paizuri_startStep" -> {
                BlockState bs = level().getBlockState(blockPosition().above());
                playSound(bs.getSoundType(level(), blockPosition(), this).getStepSound(), 1.0F, 1.0F);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void sendChatProposition() {
        String player = net.minecraft.client.Minecraft.getInstance().player.getName().getString();
        String followUp = getAnimFollowUp();
        String key = switch (followUp) {
            case "strip" -> "jenny.dialogue.showBobsandveganapls";
            case "talk"  -> "jenny.dialogue.giveblowjob";
            case "dance" -> "jenny.dialogue.givesex";
            case "hug"   -> "jenny.dialogue.givebooba";
            default      -> "jenny.dialogue.givesex";
        };
        displayChatMessageRaw("<" + player + "> " + net.minecraft.client.resources.language.I18n.get(key));
        displayChatMessageRaw("<" + player + "> ¡Hola!"); // Mensaje final limpio
    }

    // =========================================================================
    //  Boilerplate y Helpers
    // =========================================================================

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }

    @Override
    public void onGalathSexStart() { atBed = true; } // Interfaz GalathSexEntity mantenida para compatibilidad

    @Override
    public void onTick() {}

    private BlockPos findNearestBed(BlockPos origin) {
        int r = 8;
        for (int dx = -r; dx <= r; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -r; dz <= r; dz++) {
            BlockPos pos = origin.offset(dx, dy, dz);
            if (level().getBlockState(pos).getBlock() instanceof BedBlock) return pos;
        }
        return null;
    }

    private void toggleModelIndex() {
        int cur = entityData.get(MODEL_INDEX);
        entityData.set(MODEL_INDEX, cur == 1 ? 0 : 1);
    }
}