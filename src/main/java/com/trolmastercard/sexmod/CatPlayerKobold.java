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
 * CatPlayerKobold - ported from ed.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Touch boobs: TOUCH_BOOBS_INTRO - SLOW - FAST - CUM
 * Cowgirl sitting: COWGIRL_SITTING_INTRO - SLOW - FAST - CUM
 * Scale 1.6, eye height 1.34. Hand model: CatHandModel (bf).
 * Has fishing animations and "throw away" item mechanic.
 * ap field (boolean) - alternates touch_boobs_slow variant (slow vs slow1).
 */
public class CatPlayerKobold extends PlayerKoboldEntity {

    boolean flyAlt   = false;
    boolean slowVariant = false; // ap in original  toggles touch_boobs_slow1
    int attackCounter = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public CatPlayerKobold(Level level) { super(level); }
    public CatPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.6F; }
    @Override public float getEyeHeight(net.minecraft.world.entity.Pose p) { return 1.34F; }
    @Override public NpcHandModel createHandModel(int slot) { return new CatHandModel(); }
    @Override public String getHandTexturePath(int slot) { return "textures/entity/cat/hand.png"; }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        if ("action.names.touchboobs".equals(action)) {
            setSubAnimState(0, AnimState.TOUCH_BOOBS_INTRO);
            setAnimStateFiltered(AnimState.TOUCH_BOOBS_INTRO);
            getEntityData().set(TARGET_ID_PARAM, 0);
            setPartnerUUID(playerId);
        }
    }

    @Override
    public boolean onPlayerInteract(net.minecraft.world.entity.player.Player player) {
        openActionMenu(player, this, new String[]{ "action.names.touchboobs" }, false);
        return true;
    }

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        if (cur == AnimState.TOUCH_BOOBS_CUM  && (next == AnimState.TOUCH_BOOBS_FAST  || next == AnimState.TOUCH_BOOBS_SLOW))  return;
        if (cur == AnimState.COWGIRL_SITTING_CUM && (next == AnimState.COWGIRL_SITTING_FAST || next == AnimState.COWGIRL_SITTING_SLOW)) return;
        super.setAnimStateFiltered(next);
    }

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.TOUCH_BOOBS_SLOW)   return AnimState.TOUCH_BOOBS_FAST;
        if (cur == AnimState.COWGIRL_SITTING_SLOW) return AnimState.COWGIRL_SITTING_FAST;
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.TOUCH_BOOBS_SLOW || cur == AnimState.TOUCH_BOOBS_FAST)     return AnimState.TOUCH_BOOBS_CUM;
        if (cur == AnimState.COWGIRL_SITTING_SLOW || cur == AnimState.COWGIRL_SITTING_FAST) return AnimState.COWGIRL_SITTING_CUM;
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes",     5, s -> { s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null) ? RawAnimation.begin().thenLoop("animation.cat.blink") : RawAnimation.begin().thenLoop("animation.cat.null")); return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<CatPlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.null")); return PlayState.CONTINUE; }
        if (getAnimState() == AnimState.SIT) s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.sit"));
        else if (isFallFlying)               s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.fly" + (flyAlt ? "2" : "")));
        else if (s.isMoving())               s.setAndContinue(RawAnimation.begin().thenLoop(getDeltaMovement().horizontalDistanceSqr() > 0.04 ? "animation.cat.run" : "animation.cat.fastwalk"));
        else                                 s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<CatPlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case ATTACK              -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.attack" + attackCounter));
            case SIT                 -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.sit"));
            case BOW_CHARGE          -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.bowcharge"));
            case THROW_PEARL         -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.throwpearl"));
            case DOWNED              -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.downed"));
            case FISHING_START       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.start_fishing"));
            case FISHING_IDLE        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.idle_fishing"));
            case FISHING_EAT         -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.eat_fishing"));
            case THROW_AWAY          -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.throw_away"));
            case PAYMENT             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.payment"));
            case TOUCH_BOOBS_INTRO   -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.touch_boobs_intro"));
            case TOUCH_BOOBS_SLOW    -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.touch_boobs_slow" + (slowVariant ? "1" : "")));
            case TOUCH_BOOBS_FAST    -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.touch_boobs_fast"));
            case TOUCH_BOOBS_CUM     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.touch_boobs_cum"));
            case WAIT_CAT            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.wait"));
            case COWGIRL_SITTING_INTRO -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.sitting_intro"));
            case COWGIRL_SITTING_SLOW  -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.sitting_slow"));
            case COWGIRL_SITTING_FAST  -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.sitting_fast"));
            case COWGIRL_SITTING_CUM   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.sitting_cum"));
            case HEAD_PAT            -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.head_pat"));
            default                  -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.cat.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
