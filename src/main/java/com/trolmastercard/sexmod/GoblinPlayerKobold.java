package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * GoblinPlayerKobold - ported from eq.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Extends PlayerKoboldEntity and also implements the NpcInventoryCapable interface.
 * This is the renderer-side kobold that maps to GoblinEntity body animations.
 *
 * Carry chain:   PICK_UP - SHOULDER_IDLE - START_THROWING - THROWN
 * Nelson chain:  NELSON_INTRO - NELSON_SLOW - NELSON_FAST - NELSON_CUM
 * Breeding:      BREEDING_INTRO_1/2/3 - BREEDING_SLOW_1/3 - BREEDING_FAST_1/3 - BREEDING_CUM_1/2/3
 * Paizuri:       PAIZURI_START - PAIZURI_SLOW - PAIZURI_FAST - PAIZURI_CUM
 *
 * Queen goblin has pregnancy variant with breeding_2.
 * Movement has "running" vs "walk" (no fastwalk).
 * isQueen flag: enables breeding variant 2.
 */
public class GoblinPlayerKobold extends PlayerKoboldEntity implements NpcInventoryCapable {

    public boolean isQueen = false;
    int attackCounter = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public GoblinPlayerKobold(Level level) { super(level); }
    public GoblinPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public NpcHandModel createHandModel(int slot) { return new GoblinHandModel(); }
    @Override public String getHandTexturePath(int slot) { return "textures/entity/goblin/hand.png"; }

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.NELSON_SLOW)  return AnimState.NELSON_FAST;
        if (cur == AnimState.PAIZURI_SLOW) return AnimState.PAIZURI_FAST;
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.NELSON_SLOW   || cur == AnimState.NELSON_FAST)   return AnimState.NELSON_CUM;
        if (cur == AnimState.PAIZURI_SLOW  || cur == AnimState.PAIZURI_FAST)  return AnimState.PAIZURI_CUM;
        if (cur == AnimState.BREEDING_SLOW || cur == AnimState.BREEDING_FAST) return AnimState.BREEDING_CUM;
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes", 5, s -> { s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null) ? RawAnimation.begin().thenLoop("animation.goblin.blink") : RawAnimation.begin().thenLoop("animation.goblin.null")); return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<GoblinPlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.null")); return PlayState.CONTINUE; }
        if (a == AnimState.SIT)  s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.sit"));
        else if (isFallFlying)   s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.fly"));
        else if (s.isMoving())   s.setAndContinue(RawAnimation.begin().thenLoop(getDeltaMovement().horizontalDistanceSqr()>0.04 ? "animation.goblin.running" : "animation.goblin.walk"));
        else                     s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<GoblinPlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case SHOULDER_IDLE     -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.shoulder_idle"));
            case PICK_UP           -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.pick_up_person"));
            case START_THROWING    -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.throw_person"));
            case THROWN            -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.thrown"));
            case STAND_UP          -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.stand_up"));
            case STRIP             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.strip"));
            case ATTACK            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.attack"));
            case BOW_CHARGE        -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.bowcharge"));
            case SIT               -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.sit"));
            case NELSON_INTRO      -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.nelson_intro"));
            case NELSON_SLOW       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.nelson_slow"));
            case NELSON_FAST       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.nelson_fast"));
            case NELSON_CUM        -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.nelson_cum"));
            case BREEDING_INTRO    -> s.setAndContinue(RawAnimation.begin().thenPlay(isQueen ? "animation.goblin.breeding_intro_2" : "animation.goblin.breeding_intro_1"));
            case BREEDING_SLOW     -> s.setAndContinue(RawAnimation.begin().thenLoop(isQueen ? "animation.goblin.breeding_slow_3" : "animation.goblin.breeding_slow_1"));
            case BREEDING_FAST     -> s.setAndContinue(RawAnimation.begin().thenLoop(isQueen ? "animation.goblin.breeding_fast_3" : "animation.goblin.breeding_fast_1"));
            case BREEDING_CUM      -> s.setAndContinue(RawAnimation.begin().thenPlay(isQueen ? "animation.goblin.breeding_cum_2" : "animation.goblin.breeding_cum_1"));
            case BREEDING_LOOP     -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.breeding_2"));
            case PAIZURI_START     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.paizuri_start"));
            case PAIZURI_SLOW      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_slow"));
            case PAIZURI_FAST      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_fast"));
            case PAIZURI_IDLE      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_idle"));
            case PAIZURI_CUM       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.paizuri_cum"));
            default                -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
