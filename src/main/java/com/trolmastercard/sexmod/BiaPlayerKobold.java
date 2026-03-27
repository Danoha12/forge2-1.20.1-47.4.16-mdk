package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcHandModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BiaPlayerKobold - ported from eg.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Anal chain:      ANAL_PREPARE - ANAL_WAIT - ANAL_START - ANAL_SLOW - ANAL_FAST - ANAL_CUM
 * Prone doggy:    PRONE_DOGGY_INTRO - INSERT - SOFT - HARD(variant aq) - CUM
 * Also: TALK_HORNY/IDLE/RESPONSE, SITDOWN/SITDOWNIDLE, HEAD_PAT.
 * Scale 1.5. Eye height 1.5. No fly override.
 * ap = flyAlt bool. aq = prone_doggy_hard variant suffix. S = attackCounter.
 */
public class BiaPlayerKobold extends PlayerKoboldEntity {

    boolean flyAlt = false;
    int aq = 1; // prone_doggy_hard variant
    int attackCounter = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BiaPlayerKobold(Level level) { super(level); }
    public BiaPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.5F; }
    @Override public float getEyeHeight(net.minecraft.world.entity.Pose p) { return 1.5F; }
    @Override public NpcHandModel createHandModel(int slot) { return new BiaHandModel(); }
    @Override public String getHandTexturePath(int slot) { return "textures/entity/bia/hand.png"; }

    @Override
    public boolean onSexActionRequest(String action) {
        switch (action) {
            case "anal"  -> { setAnimStateFiltered(AnimState.ANAL_PREPARE); setSubAnimState(0, AnimState.ANAL_PREPARE); return true; }
            case "doggy" -> { setAnimStateFiltered(AnimState.SITDOWN);      setSubAnimState(0, AnimState.SITDOWN);      return true; }
        }
        return false;
    }

    @Override
    public boolean onPlayerInteract(net.minecraft.world.entity.player.Player player) {
        openActionMenu(player, this, new String[]{ "anal", "doggy" }, false);
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

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.ANAL_SLOW)              return AnimState.ANAL_FAST;
        if (cur == AnimState.PRONE_DOGGY_SOFT)       return AnimState.PRONE_DOGGY_HARD;
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.ANAL_SLOW || cur == AnimState.ANAL_FAST) return AnimState.ANAL_CUM;
        if (cur == AnimState.PRONE_DOGGY_SOFT || cur == AnimState.PRONE_DOGGY_HARD) return AnimState.PRONE_DOGGY_CUM;
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes", 5, s -> {
                s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null)
                        ? RawAnimation.begin().thenLoop("animation.bia.fhappy")
                        : RawAnimation.begin().thenLoop("animation.bia.null"));
                return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<BiaPlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.null")); return PlayState.CONTINUE; }
        if (a == AnimState.SIT)  s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sit"));
        else if (isFallFlying)   s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.fly" + (flyAlt ? "2" : "")));
        else if (s.isMoving())   s.setAndContinue(RawAnimation.begin().thenLoop(getDeltaMovement().horizontalDistanceSqr()>0.04 ? "animation.bia.run" : "animation.bia.fastwalk"));
        else                     s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<BiaPlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case STRIP                  -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.strip"));
            case ATTACK                 -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.attack" + attackCounter));
            case BOW_CHARGE             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.bowcharge"));
            case RIDE                   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.ride"));
            case SIT                    -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sit"));
            case THROW_PEARL            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.throwpearl"));
            case DOWNED                 -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.downed"));
            case TALK_HORNY             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.talk_horny"));
            case TALK_IDLE              -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.talk_idle"));
            case TALK_RESPONSE          -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.talk_response"));
            case ANAL_PREPARE           -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.anal_prepare"));
            case ANAL_WAIT              -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_wait"));
            case ANAL_START             -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_start"));
            case ANAL_SLOW              -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_slow"));
            case ANAL_FAST              -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_fast"));
            case ANAL_CUM               -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.anal_cum"));
            case HEAD_PAT               -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.headpat"));
            case SITDOWN                -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.sitdown"));
            case SITDOWN_IDLE           -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sitdownidle"));
            case PRONE_DOGGY_INTRO      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_intro"));
            case PRONE_DOGGY_INSERT     -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_insert"));
            case PRONE_DOGGY_SOFT       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_soft"));
            case PRONE_DOGGY_HARD       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_hard" + aq));
            case PRONE_DOGGY_CUM        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_cum"));
            default                     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
