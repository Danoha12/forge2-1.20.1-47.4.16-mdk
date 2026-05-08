package com.trolmastercard.sexmod.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;           // Para que reconozca a la clase madre
import com.trolmastercard.sexmod.entity.BaseNpcEntity;               // Para que reconozca la base
import com.trolmastercard.sexmod.client.model.NpcGirlInterface; // El "GPS" para encontrar la interfaz
import java.util.UUID;

/**
 * EllieEntity - ported from el.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Full NPC entity (extends NpcInventoryEntity). Uses "ellie" animation prefix.
 */
public class EllieEntity extends NpcInventoryEntity implements NpcGirlInterface {

    // -- Constants --------------------------------------------------------------
    static final float LIGHT_RADIUS = 10.0F;
    static final int   TIMER_A      = 16;
    static final int   TIMER_B      = 79;
    static final int   TIMER_C      = 109;
    static final int   TIMER_D      = 150;
    static final int   TIMER_E      = 20;
    static final int   TIMER_F      = 110;
    static final int   SEG_COUNT    = 4;

    // -- Instance fields --------------------------------------------------------
    int hugTimer         = -1;
    boolean crouchWalk   = false;
    boolean startedSex   = false;
    boolean isCrouch     = false;
    int timerAf          = -1;
    int timerY           = -1;
    int timerAl          = -1;
    int timerAi          = -1;
    boolean sexPending   = false;
    Object[] pendingArgs = null;
    int timerZ           = -1;
    int carrySlowVariant = 1;
    boolean actionSent   = false;

    // Variable para guardar la animación de seguimiento
    private String currentFollowUpAnim = "";

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public EllieEntity(EntityType<? extends EllieEntity> type, Level level) {
        super(type, level);
    }

    // -- NPC identity -----------------------------------------------------------

    public String getNpcName() {
        return "Ellie";
    }


    public void onSetHome(BlockPos pos) {
        sendNpcMessage("Okay, I will be residing here then..");
        playSound(ModSounds.GIRLS_ELLIE_HUH[0].get(), 6.0F, 1.0F);
    }

    // -- Hitbox / eye height ----------------------------------------------------

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dimensions) {
        return isCrouchingCheck() ? 1.53F : 1.9F;
    }


    /** Returns true if the block 2 above the entity's feet is non-air. */
    boolean isCrouchingCheck() {
        if (getAnimState() != AnimState.NULL) return false;
        return !level().isEmptyBlock(blockPosition().above(2));
    }

    @Override
    public float getModelScale() {
        return 0.4F;
    }

    // -- Sex interactions -------------------------------------------------------


    public boolean onPlayerInteract(Player player, boolean hasPartner) {
        if (hasPartner) {
            openActionMenu(player, this, new String[]{ "action.names.cowgirl", "action.names.missionary" }, false);
            return true;
        }
        if (entityData.get(BaseNpcEntity.MODEL_INDEX) == 0) {
            openActionMenu(player, this, new String[]{ "action.names.dressup" }, true);
            return true;
        }
        openActionMenu(player, this, new String[]{ "Face fuck" }, true);
        return true;
    }


    @Override
    public void triggerAction(String action, UUID playerId) {
        // 🚨 Llamamos al super con el nombre correcto
        super.triggerAction(action, playerId);

        this.actionSent = true;
        switch (action) {
            case "action.names.missionary" -> {
                setAnimStateFiltered(AnimState.HUGSELECTED);
                setSubAnim("animationFollowUp", "Missionary");
            }
            case "action.names.cowgirl" -> {
                setAnimStateFiltered(AnimState.HUGSELECTED);
                setSubAnim("animationFollowUp", "cowgirl");
            }
            case "action.names.dressup", "action.names.strip" -> {
                setAnimStateFiltered(AnimState.STRIP);
                setSubAnim("animationFollowUp", "");
            }
            case "Face fuck" -> {
                sendFaceFuckRequest(playerId);
            }
        }
    }

    @Override
    public void stopFollow() {
        super.stopFollow();
        sendNpcMessage("stay safe darling~");
        playSound(ModSounds.GIRLS_ELLIE_SIGH[1].get(), 6.0F, 1.0F);
    }

    // -- State guards -----------------------------------------------------------

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        if (next == AnimState.HUGSELECTED && !level().isClientSide()) hugTimer = TIMER_B;

        if (cur == AnimState.MISSIONARY_CUM && (next == AnimState.MISSIONARY_FAST || next == AnimState.MISSIONARY_SLOW)) return;
        if (cur == AnimState.COWGIRLCUM     && (next == AnimState.COWGIRLFAST     || next == AnimState.COWGIRLSLOW))     return;
        if (cur == AnimState.CARRY_CUM      && (next == AnimState.CARRY_FAST      || next == AnimState.CARRY_SLOW))      return;

        if (next == AnimState.CARRY_INTRO)  hugTimer = 0;
        super.setAnimStateFiltered(next);
    }

    public boolean canCarry() {
        return getAnimState() != AnimState.CARRY_INTRO;
    }

    // -- Tick -------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        tickCarryAnim();
        tickSexControl();
    }

    void tickCarryAnim() {
        if (hugTimer == -1) return;
        if (getAnimState() != AnimState.CARRY_SLOW) return;
        // Asumiendo que SexAnimationManager existe en BaseNpcEntity o utilidades
        // SexAnimationManager.incrementTick();
    }

    void tickSexControl() {
        // if (!SexAnimationManager.isActive()) return;
        // if (getAnimState() == AnimState.CARRY_SLOW) SexAnimationManager.incrementTick();
    }

    // -- Animation setup (GeckoLib 4) -------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "eyes", 5, s -> {
                    s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null)
                            ? RawAnimation.begin().thenLoop("animation.ellie.eyes")
                            : RawAnimation.begin().thenLoop("animation.ellie.null"));
                    return PlayState.CONTINUE; }),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<EllieEntity> s) {
        AnimState a = getAnimState();
        boolean crouching = isCrouchingCheck();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.null")); return PlayState.CONTINUE; }
        if (a == AnimState.RIDE)      s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.ride"));
        else if (s.isMoving()) {
            double spd = getDeltaMovement().horizontalDistanceSqr();
            if (spd > 0.04) s.setAndContinue(RawAnimation.begin().thenLoop(crouching ? "animation.ellie.crouchwalk" : "animation.ellie.run"));
            else            s.setAndContinue(RawAnimation.begin().thenLoop(crouching ? "animation.ellie.crouchwalk" : "animation.ellie.fastwalk"));
        } else                      s.setAndContinue(RawAnimation.begin().thenLoop(crouching ? "animation.ellie.crouchidle" : "animation.ellie.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<EllieEntity> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case STRIP             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.strip"));
            case DASH              -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.dash"));
            case HUG               -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.hug"));
            case HUGIDLE           -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.hugidle"));
            case HUGSELECTED       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.hugselected"));
            case SITDOWN           -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.sitdown"));
            case SITDOWNIDLE       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.sitdownidle"));
            case COWGIRLSTART      -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.cowgirlstart"));
            case COWGIRLSLOW       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlslow2"));
            case COWGIRLFAST       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlfast"));
            case COWGIRLCUM        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlcum"));
            case ATTACK            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.attack1"));
            case BOW               -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.bowcharge"));
            case RIDE              -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.ride"));
            case SIT               -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.sit"));
            case THROW_PEARL       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.throwpearl"));
            case DOWNED            -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.downed"));
            case MISSIONARY_START  -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.missionary_start"));
            case MISSIONARY_SLOW   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.missionary_slow"));
            case MISSIONARY_FAST   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.missionary_fast"));
            case MISSIONARY_CUM    -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.missionary_cum"));
            case CARRY_INTRO       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.carry_intro"));
            case CARRY_SLOW        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.carry_slow" + carrySlowVariant));
            case CARRY_FAST        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.carry_fast"));
            case CARRY_CUM         -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.carry_cum"));
            default                -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.null"));
        }
        return PlayState.CONTINUE;
    }

    // -- Utilidades / Herramientas de Reparación --------------------------------

    public void setSubAnim(String key, String value) {
        if (key.equals("animationFollowUp")) {
            this.currentFollowUpAnim = value;
        }
    }

    public void sendFaceFuckRequest(UUID playerId) {
        // Aquí debe ir el paquete de red que le dice al cliente que bloquee la cámara
    }

    // 🚨 REPARADO: ¡Conexión lista a tu NpcActionScreen real!
    public void openActionMenu(Player player, EllieEntity npc, String[] actions, boolean isDressUp) {
        if (this.level().isClientSide()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.trolmastercard.sexmod.client.gui.NpcActionScreen(
                            npc, player, actions, null, isDressUp
                    )
            );
        }
    }

    public void sendNpcMessage(String text) {
        if (!this.level().isClientSide()) {
            java.util.List<net.minecraft.world.entity.player.Player> players = this.level().getEntitiesOfClass(
                    net.minecraft.world.entity.player.Player.class,
                    this.getBoundingBox().inflate(10.0D)
            );
            for (net.minecraft.world.entity.player.Player player : players) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§d<" + this.getNpcName() + ">§f " + text));
            }
        }
    }

    @Override
    public net.minecraft.world.phys.Vec3 getBonePosition(String boneName) {
        return this.position().add(0, 1, 0);
    }

    // -- NBT --------------------------------------------------------------------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("carrySlowVariant", carrySlowVariant);
        tag.putString("currentFollowUpAnim", currentFollowUpAnim);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        carrySlowVariant = tag.getInt("carrySlowVariant");
        currentFollowUpAnim = tag.getString("currentFollowUpAnim");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}