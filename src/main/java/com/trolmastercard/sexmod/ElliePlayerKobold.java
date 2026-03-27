package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * ElliePlayerKobold - ported from ee.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Animations: Cowgirl, Missionary, Carry/Face-fuck, HUG, SIT_DOWN.
 * Scale 2.05; eye height varies: 1.53 crouched, 1.9 standing.
 * Hand model: EllieHandModel (cf). Textures: hand.png / hand_nude.png.
 * p() = true - can animate "animationFollowUp" sequences.
 *
 * State guards:
 *   MISSIONARY_CUM - blocks MISSIONARY_FAST / SLOW
 *   COWGIRLCUM     - blocks COWGIRLFAST / SLOW
 *   CARRY_CUM      - blocks CARRY_FAST / SLOW
 *
 * Progression:
 *   COWGIRLSLOW - COWGIRLFAST - COWGIRLCUM
 *   MISSIONARY_SLOW - MISSIONARY_FAST - MISSIONARY_CUM
 *   CARRY_SLOW - CARRY_FAST - CARRY_CUM
 */
public class ElliePlayerKobold extends PlayerKoboldEntity {

    boolean flyAlt = false;
    boolean ar = false; // ar field  alternate carry_slow variant
    int attackCounter = 1;
    int carrySlowVariant = 1; // ap field  carry_slow suffix
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public ElliePlayerKobold(Level level) { super(level); }
    public ElliePlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 2.05F; }
    @Override public float getEyeHeight(net.minecraft.world.entity.Pose p) { return isCrouching() ? 1.53F : 1.9F; }
    @Override public boolean autoUnlockSex() { return false; }
    @Override public NpcHandModel createHandModel(int slot) { return new EllieHandModel(); }
    @Override public String getHandTexturePath(int slot) { return slot == 0 ? "textures/entity/ellie/hand_nude.png" : "textures/entity/ellie/hand.png"; }
    @Override public boolean canQueueFollowUp() { return true; }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "action.names.cowgirl" -> queueFollowUp("Cowgirl");
            case "action.names.missionary" -> queueFollowUp("Missionary");
            case "Face fuck" -> {
                setPartnerUUID(playerId);
                setAnimStateFiltered(AnimState.CARRY_INTRO);
                setSubAnimState(getArmHeightSlot(), AnimState.CARRY_INTRO);
            }
        }
        // Send queued action to server
        if (getOwnerUUID() != null) {
            ModNetwork.CHANNEL.sendToServer(new NpcActionQueuePacket(action, playerId, getOwnerUUID(), actionSent));
            actionSent = true;
        }
    }

    boolean actionSent = false;

    @Override
    public boolean onPlayerInteract(net.minecraft.world.entity.player.Player player) {
        openActionMenu(player, this, new String[]{ "Face fuck" }, false);
        return true;
    }

    public void openCowgirlMenu(net.minecraft.world.entity.player.Player player) {
        openActionMenu(player, this, new String[]{ "action.names.cowgirl", "action.names.missionary" }, false);
    }

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        if (cur == AnimState.MISSIONARY_CUM && (next == AnimState.MISSIONARY_FAST || next == AnimState.MISSIONARY_SLOW)) return;
        if (cur == AnimState.COWGIRLCUM     && (next == AnimState.COWGIRLFAST     || next == AnimState.COWGIRLSLOW))     return;
        if (cur == AnimState.CARRY_CUM      && (next == AnimState.CARRY_FAST      || next == AnimState.CARRY_SLOW))      return;
        super.setAnimStateFiltered(next);
    }

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.COWGIRLSLOW)    return AnimState.COWGIRLFAST;
        if (cur == AnimState.MISSIONARY_SLOW) return AnimState.MISSIONARY_FAST;
        if (cur == AnimState.CARRY_SLOW)     return AnimState.CARRY_FAST;
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.COWGIRLSLOW  || cur == AnimState.COWGIRLFAST)     return AnimState.COWGIRLCUM;
        if (cur == AnimState.MISSIONARY_SLOW || cur == AnimState.MISSIONARY_FAST) return AnimState.MISSIONARY_CUM;
        if (cur == AnimState.CARRY_SLOW   || cur == AnimState.CARRY_FAST)      return AnimState.CARRY_CUM;
        return null;
    }

    public void sitDown() { setAnimStateFiltered(AnimState.SITDOWN); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes",     5, s -> { s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null) ? RawAnimation.begin().thenLoop("animation.ellie.eyes") : RawAnimation.begin().thenLoop("animation.ellie.null")); return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<ElliePlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.null")); return PlayState.CONTINUE; }
        if (a == AnimState.RIDE)     s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.ride"));
        else if (isFallFlying)       s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.fly" + (ar ? "2" : "")));
        else if (s.isMoving()) {
            double spd = getDeltaMovement().horizontalDistanceSqr();
            if (spd > 0.04) s.setAndContinue(RawAnimation.begin().thenLoop(isCrouching() ? "animation.ellie.crouchwalk" : "animation.ellie.run"));
            else            s.setAndContinue(RawAnimation.begin().thenLoop(isCrouching() ? "animation.ellie.crouchwalk" : "animation.ellie.fastwalk"));
        } else                       s.setAndContinue(RawAnimation.begin().thenLoop(isCrouching() ? "animation.ellie.crouchidle" : "animation.ellie.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<ElliePlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case STRIP             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.strip"));
            case DASH              -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.dash"));
            case HUG               -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.hug"));
            case HUG_IDLE          -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.hugidle"));
            case HUG_SELECTED      -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.hugselected"));
            case SITDOWN           -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.sitdown"));
            case SITDOWN_IDLE      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.sitdownidle"));
            case COWGIRLSTART      -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.cowgirlstart"));
            case COWGIRLSLOW       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlslow2"));
            case COWGIRLFAST       -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlfast"));
            case COWGIRLCUM        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlcum"));
            case ATTACK            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.attack" + attackCounter));
            case BOW_CHARGE        -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.bowcharge"));
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

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
