package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.registry.SoundCatalog; // Asegúrate de que este es tu registro de sonidos
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * EllieEntity - Compañera de la Tribu.
 * Portado a 1.20.1 / GeckoLib 4.
 * * Extiende NpcInventoryEntity.
 * * Mecánica única: Detecta bloques arriba de su cabeza para agacharse automáticamente.
 * * Múltiples rutas de minijuegos e interacción.
 */
public class EllieEntity extends NpcInventoryEntity implements NpcGirlInterface {

    // =========================================================================
    //  Constantes
    // =========================================================================

    static final float LIGHT_RADIUS = 10.0F;
    static final int   TIMER_A      = 16;
    static final int   TIMER_B      = 79;
    static final int   TIMER_C      = 109;
    static final int   TIMER_D      = 150;
    static final int   TIMER_E      = 20;
    static final int   TIMER_F      = 110;
    static final int   SEG_COUNT    = 4;

    // =========================================================================
    //  Campos de Instancia
    // =========================================================================

    int hugTimer         = -1;
    boolean crouchWalk   = false;
    boolean interactionStarted = false; // Antes startedSex
    boolean isCrouch     = false;
    int timerAf          = -1;
    int timerY           = -1;
    int timerAl          = -1;
    int timerAi          = -1;
    boolean actionPending= false; // Antes sexPending
    Object[] pendingArgs = null;
    int timerZ           = -1;
    int carrySlowVariant = 1;
    boolean actionSent   = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    // =========================================================================
    //  Constructor
    // =========================================================================

    public EllieEntity(EntityType<? extends EllieEntity> type, Level level) {
        super(type, level);
        // Hitbox delgada
        // En 1.20.1 esto suele definirse en el EntityType de registro,
        // pero podemos intentar forzarlo con refreshDimensions si es necesario.
    }

    // =========================================================================
    //  Identidad NPC y Base
    // =========================================================================

    @Override
    public String getNpcName() { return "Ellie"; }

    @Override
    public void onSetHome(BlockPos pos) {
        sendNpcMessage("Okay, I will be residing here then..");
        playNpcSound(SoundCatalog.GIRLS_ELLIE_HUH[0], 6.0F); // Ajusta SoundCatalog al real
    }

    // =========================================================================
    //  Hitbox e Inteligencia de Agachado
    // =========================================================================

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose p, net.minecraft.world.entity.EntityDimensions dim) {
        return isCrouchingCheck() ? 1.53F : 1.9F;
    }

    /** Retorna true si hay un bloque no-aire 2 bloques arriba de sus pies. */
    boolean isCrouchingCheck() {
        if (isInteractionModeActive()) return false; // Asume que isSexModeActive() se renombró en base
        return !level().isEmptyBlock(blockPosition().above(2));
    }

    @Override
    public float getModelScale() { return 0.4F; }

    // =========================================================================
    //  Interacciones y Menús
    // =========================================================================

    @Override
    public boolean onPlayerInteract(Player player, boolean hasPartner) {
        if (hasPartner) {
            openActionMenu(player, this, new String[]{ "action.names.minigame_a", "action.names.minigame_b" }, false);
            return true;
        }
        if (entityData.get(BaseNpcEntity.MODEL_INDEX) == 0) {
            openActionMenu(player, this, new String[]{ "action.names.dressup" }, true);
            return true;
        }

        // Mantenemos "Face fuck" a nivel código si el JSON o Paquete lo exige, pero lo mostramos limpio.
        openActionMenu(player, this, new String[]{ "Face fuck" }, true);
        return true;
    }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        super.onActionSelected(action, playerId);
        actionSent = true;
        switch (action) {
            case "action.names.minigame_b" -> { // Antes missionary
                setAnimState(AnimState.HUG_SELECTED); // Asume que HUGSELECTED se estandarizó
                setSubAnim("animationFollowUp", "Missionary");
            }
            case "action.names.minigame_a" -> { // Antes cowgirl
                setAnimState(AnimState.HUG_SELECTED);
                setSubAnim("animationFollowUp", "cowgirl");
            }
            case "action.names.dressup", "action.names.strip" -> {
                setAnimState(AnimState.STRIP);
                setSubAnim("animationFollowUp", "");
            }
            case "Face fuck" -> {
                // Se mantiene el string por compatibilidad de red
                sendFaceInteractionRequest(playerId); // Asegúrate de que esto exista en base
                // TODO: Desactiva el input del jugador aquí. Ej: ClientInputManager.setFrozen(true);
            }
        }
    }

    @Override
    public void stopFollow() {
        super.stopFollow();
        sendNpcMessage("stay safe darling~");
        playNpcSound(SoundCatalog.GIRLS_ELLIE_SIGH[1], 6.0F);
    }

    // =========================================================================
    //  Filtro de Transiciones de Estado
    // =========================================================================

    @Override
    public void setAnimState(AnimState next) { // Reemplaza setAnimStateFiltered
        AnimState cur = getAnimState();
        if (next == AnimState.HUG_SELECTED && !level().isClientSide()) hugTimer = TIMER_B;

        // Evitar bucles infinitos en resoluciones
        if (cur == AnimState.MINIGAME_B_FINISH && (next == AnimState.MINIGAME_B_FAST || next == AnimState.MINIGAME_B_SLOW)) return;
        if (cur == AnimState.MINIGAME_A_FINISH && (next == AnimState.MINIGAME_A_FAST || next == AnimState.MINIGAME_A_SLOW)) return;
        if (cur == AnimState.CARRY_FINISH      && (next == AnimState.CARRY_FAST      || next == AnimState.CARRY_SLOW))      return;

        if (next == AnimState.CARRY_INTRO) hugTimer = 0;
        super.setAnimState(next);
    }

    public boolean canCarry() {
        return getAnimState() != AnimState.CARRY_INTRO;
    }

    // =========================================================================
    //  Tick
    // =========================================================================

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (interactionStarted) {
                openActionMenu(net.minecraft.client.Minecraft.getInstance().player, this,
                        new String[]{ "action.names.minigame_a", "action.names.minigame_b" }, false);
                interactionStarted = false;
            }
        }
        tickCarryAnim();
        tickInteractionControl();
    }

    void tickCarryAnim() {
        if (hugTimer == -1) return;
        if (getAnimState() != AnimState.CARRY_SLOW) return;
        // TODO: Módulo de manager de animación. Ej: InteractionAnimationManager.incrementTick();
    }

    void tickInteractionControl() {
        // TODO: Módulo de manager. Ej: if (!InteractionAnimationManager.isActive()) return;
        if (getAnimState() == AnimState.CARRY_SLOW) {
            // InteractionAnimationManager.incrementTick();
        }
    }

    // =========================================================================
    //  Controladores de Animación GeckoLib 4
    // =========================================================================

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // ensureControllerInit() fue abstraído en GeckoLib4 por la propia cache
        registrar.add(
                new AnimationController<>(this, "eyes", 5, s -> {
                    s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null)
                            ? RawAnimation.begin().thenLoop("animation.ellie.eyes")
                            : RawAnimation.begin().thenLoop("animation.ellie.null"));
                    return PlayState.CONTINUE;
                }),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<EllieEntity> s) {
        AnimState a = getAnimState();
        boolean crouching = isCrouchingCheck();

        if (a != AnimState.NULL && a != null) {
            s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.null"));
            return PlayState.CONTINUE;
        }
        if (a == AnimState.RIDE) {
            s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.ride"));
        }
        else if (s.isMoving()) {
            double spd = getDeltaMovement().horizontalDistanceSqr();
            if (spd > 0.04) s.getController().setAnimation(RawAnimation.begin().thenLoop(crouching ? "animation.ellie.crouchwalk" : "animation.ellie.run"));
            else            s.getController().setAnimation(RawAnimation.begin().thenLoop(crouching ? "animation.ellie.crouchwalk" : "animation.ellie.fastwalk"));
        } else {
            s.getController().setAnimation(RawAnimation.begin().thenLoop(crouching ? "animation.ellie.crouchidle" : "animation.ellie.idle"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<EllieEntity> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;

        // Mapeo de estados enmascarados a los JSON originales
        switch (a) {
            case STRIP             -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.strip"));
            case DASH              -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.dash"));
            case CLOSE_HUG         -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.hug"));
            case CLOSE_HUG_IDLE    -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.hugidle"));
            case HUG_SELECTED      -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.hugselected"));
            case SITDOWN           -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.sitdown"));
            case SITDOWN_IDLE      -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.sitdownidle"));
            case MINIGAME_A_START  -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.cowgirlstart"));
            case MINIGAME_A_SLOW   -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.cowgirlslow2"));
            case MINIGAME_A_FAST   -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.cowgirlfast"));
            case MINIGAME_A_FINISH -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.cowgirlcum"));
            case ATTACK            -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.attack1"));
            case BOWCHARGE         -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.bowcharge"));
            case RIDE              -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.ride"));
            case SIT               -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.sit"));
            case THROW_PEARL       -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.throwpearl"));
            case DOWNED            -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.downed"));
            case MINIGAME_B_START  -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.missionary_start"));
            case MINIGAME_B_SLOW   -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.missionary_slow"));
            case MINIGAME_B_FAST   -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.missionary_fast"));
            case MINIGAME_B_FINISH -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.missionary_cum"));
            case CARRY_INTRO       -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.carry_intro"));
            case CARRY_SLOW        -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.carry_slow" + carrySlowVariant));
            case CARRY_FAST        -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.carry_fast"));
            case CARRY_FINISH      -> s.getController().setAnimation(RawAnimation.begin().thenLoop("animation.ellie.carry_cum"));
            default                -> s.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ellie.null"));
        }
        return PlayState.CONTINUE;
    }

    // =========================================================================
    //  NBT Guardado
    // =========================================================================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("carrySlowVariant", carrySlowVariant);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        carrySlowVariant = tag.getInt("carrySlowVariant");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}