package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * GalathPlayerKobold - ported from er.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Implements FlyingNpcEntity (b7). Can fly (controlled_flight).
 * Rape chain:   RAPE_INTRO - RAPE_ON_GOING(variant ar) - RAPE_CUM - RAPE_CUM_IDLE
 * Corrupt chain: CORRUPT_INTRO - CORRUPT_SLOW - CORRUPT_FAST/HARD - CORRUPT_CUM
 * Scale 2.3. Hand model: GalathHandModel (a5). Texture: textures/entity/galath/hand.png
 * ar = rape_on_going variant; as = corrupt_hard bool.
 * getCumState: CORRUPT_SLOW/FAST - CORRUPT_CUM; RAPE_ON_GOING - RAPE_CUM
 */
public class GalathPlayerKobold extends PlayerKoboldEntity implements FlyingNpcEntity {

    boolean flyAlt = false;
    int ar = 0;       // rape_on_going variant suffix
    boolean as = false; // corrupt variant: hard vs soft
    boolean aq = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public GalathPlayerKobold(Level level) { super(level); }
    public GalathPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 2.3F; }
    @Override public NpcHandModel createHandModel(int slot) { return new GalathHandModel(); }
    @Override public String getHandTexturePath(int slot) { return "textures/entity/galath/hand.png"; }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "cowgirl" -> {
                setPartnerUUID(playerId);
                setAnimStateFiltered(AnimState.RAPE_INTRO);
                setSubAnimState(getArmHeightSlot(), AnimState.RAPE_INTRO);
            }
            case "mating press" -> {
                setPartnerUUID(playerId);
                setAnimStateFiltered(AnimState.CORRUPT_SLOW);
                setSubAnimState(getArmHeightSlot(), AnimState.CORRUPT_SLOW);
                startFlight();
            }
        }
    }

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        if (cur == AnimState.CORRUPT_CUM && (next == AnimState.CORRUPT_FAST || next == AnimState.CORRUPT_SLOW)) return;
        if (cur == AnimState.RAPE_CUM    &&  next == AnimState.RAPE_ON_GOING) return;
        super.setAnimStateFiltered(next);
    }

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.CORRUPT_SLOW) return AnimState.CORRUPT_FAST;
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.CORRUPT_SLOW || cur == AnimState.CORRUPT_FAST) return AnimState.CORRUPT_CUM;
        if (cur == AnimState.RAPE_ON_GOING) return AnimState.RAPE_CUM;
        return null;
    }

    public void startFlight() { /* handled by GalathEntity flight controller */ }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes", 5, s -> {
                s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null)
                        ? RawAnimation.begin().thenLoop("animation.galath.blink")
                        : RawAnimation.begin().thenLoop("animation.galath.null"));
                return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<GalathPlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.null")); return PlayState.CONTINUE; }
        if (a == AnimState.SIT)    s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.sit"));
        else if (isFallFlying)     s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.controlled_flight"));
        else if (s.isMoving()) {
            double spd = getDeltaMovement().horizontalDistanceSqr();
            s.setAndContinue(RawAnimation.begin().thenLoop(isCrouching() ? "animation.galath.crouchwalk" : (spd > 0.04 ? "animation.galath.run" : "animation.galath.walk")));
        } else                     s.setAndContinue(RawAnimation.begin().thenLoop(isCrouching() ? "animation.galath.crouchidle" : "animation.galath.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<GalathPlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case STRIP              -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.strip"));
            case ATTACK             -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.attack1"));
            case BOW_CHARGE         -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.bowcharge"));
            case SIT                -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.sit"));
            case RAPE_INTRO         -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape_intro"));
            case RAPE_ON_GOING      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape" + ar));
            case RAPE_CUM           -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape_cum"));
            case RAPE_CUM_IDLE      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape_cum_idle"));
            case CORRUPT_INTRO      -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.galath.corrupt_intro"));
            case CORRUPT_SLOW       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.corrupt_slow"));
            case CORRUPT_FAST       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.corrupt_" + (as ? "hard" : "soft")));
            case CORRUPT_CUM        -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.galath.corrupt_cum"));
            case FLY                -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.controlled_flight"));
            default                 -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.galath.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
