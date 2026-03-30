package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BiaPlayerKobold — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * Variante de acompañante para el NPC Bia.
 * Secuencias principales:
 * - BACK (Mapeado de Anal): PREP -> WAIT -> START -> SLOW -> FAST -> FINISH.
 * - PRONE_DANCE (Mapeado de Prone Doggy): INTRO -> INSERT -> SOFT -> HARD -> FINISH.
 */
public class BiaPlayerKobold extends PlayerKoboldEntity {

    private boolean flyAlt = false;
    private int aq = 1; // Sufijo para variante PRONE_DANCE_HARD
    private int attackCounter = 1;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BiaPlayerKobold(Level level) { super(level); }
    public BiaPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.5F; }

    @Override
    public float getEyeHeight(Pose pose) { return 1.5F; }

    // @Override // Descomentar cuando NpcHandModel sea porteado
    // public NpcHandModel createHandModel(int slot) { return new BiaHandModel(); }

    // @Override // Descomentar si existe en PlayerKoboldEntity
    // public String getHandTexturePath(int slot) { return "textures/entity/bia/hand.png"; }

    // @Override // Descomentar si el método original es onSexActionRequest
    public boolean onInteractiveActionRequest(String action) {
        switch (action) {
            case "back" -> { // Antes "anal"
                setAnimStateFiltered(AnimState.BACK_PREP);
                setSubAnimState(0, AnimState.BACK_PREP);
                return true;
            }
            case "dance" -> { // Antes "doggy"
                setAnimStateFiltered(AnimState.SITDOWN);
                setSubAnimState(0, AnimState.SITDOWN);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        // Asumiendo que openActionMenu está en BaseNpcEntity
        openActionMenu(player, this, new String[]{ "back", "dance" }, false);
        return true;
    }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        if ("action.names.headpat".equals(action)) {
            setPartnerUUID(playerId);
            setAnimStateFiltered(AnimState.HEAD_PAT);
            setSubAnimState(getArmHeightSlot(), AnimState.HEAD_PAT);
        }
    }

    @Override
    public AnimState getNextState(AnimState current) {
        if (current == AnimState.BACK_SLOW)        return AnimState.BACK_FAST;
        if (current == AnimState.PRONE_DANCE_SOFT)  return AnimState.PRONE_DANCE_HARD;
        return null;
    }

    @Override
    public AnimState getCumState(AnimState current) {
        if (current == AnimState.BACK_SLOW || current == AnimState.BACK_FAST)
            return AnimState.BACK_FINISH;
        if (current == AnimState.PRONE_DANCE_SOFT || current == AnimState.PRONE_DANCE_HARD)
            return AnimState.PRONE_DANCE_FINISH;
        return null;
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
                new AnimationController<>(this, "eyes", 5, state -> {
                    AnimState as = getAnimState();
                    if (as == AnimState.NULL || as == null) {
                        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.fhappy"));
                    }
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.null"));
                }),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action", 0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<BiaPlayerKobold> state) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.null"));
        }

        if (a == AnimState.SIT) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sit"));

        // isFallFlying() es el método estándar en 1.20.1 para las Elitras / Vuelo
        if (this.isFallFlying()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.fly" + (flyAlt ? "2" : "")));
        }

        if (state.isMoving()) {
            String anim = getDeltaMovement().horizontalDistanceSqr() > 0.04 ? "animation.bia.run" : "animation.bia.fastwalk";
            return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
        }

        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.idle"));
    }

    private PlayState actionController(AnimationState<BiaPlayerKobold> state) {
        AnimState a = getAnimState();
        if (a == null) return PlayState.CONTINUE;

        // Mapeo SFW -> Archivos JSON originales
        switch (a) {
            case STRIP                  -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.strip"));
            case ATTACK                 -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.attack" + attackCounter));
            case BOW                    -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.bowcharge"));
            case RIDE                   -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.ride"));
            case SIT                    -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sit"));
            case THROW_PEARL            -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.throwpearl"));
            case DOWNED                 -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.downed"));

            case TALK_EXCITED           -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.talk_horny"));
            case TALK_IDLE              -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.talk_idle"));
            case TALK_RESPONSE          -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.talk_response"));

            // Secuencia BACK (Anal)
            case BACK_PREP              -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.anal_prepare"));
            case BACK_WAIT              -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_wait"));
            case BACK_START             -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_start"));
            case BACK_SLOW              -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_slow"));
            case BACK_FAST              -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_fast"));
            case BACK_FINISH            -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.anal_cum"));

            case HEAD_PAT               -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.headpat"));
            case SITDOWN                -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.sitdown"));
            case SITDOWNIDLE            -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sitdownidle"));

            // Secuencia PRONE_DANCE (Prone Doggy)
            case PRONE_DANCE_INTRO      -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_intro"));
            case PRONE_DANCE_INSERT     -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_insert"));
            case PRONE_DANCE_SOFT       -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_soft"));
            case PRONE_DANCE_HARD       -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_hard" + aq));
            case PRONE_DANCE_FINISH     -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_cum"));

            default                     -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.null"));
        }
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}