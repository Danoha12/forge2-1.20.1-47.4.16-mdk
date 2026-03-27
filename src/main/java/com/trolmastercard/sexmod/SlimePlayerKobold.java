package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * SlimePlayerKobold - ported from ec.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Blowjob chain: SUCKBLOWJOB - THRUSTBLOWJOB - CUMBLOWJOB
 * Doggy chain:   DOGGYSLOW - DOGGYFAST - DOGGYCUM
 * WAITDOGGY: waits at bed, grabs player when close.
 * Cannot fly (canFly=false). Eye height 1.64. Scale 1.6.
 */
public class SlimePlayerKobold extends PlayerKoboldEntity {

    boolean flyAlt = false;
    int attackCounter = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public SlimePlayerKobold(Level level) { super(level); }
    public SlimePlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.6F; }
    @Override public float getEyeHeight(net.minecraft.world.entity.Pose p) { return 1.64F; }
    @Override public boolean canFly() { return false; }
    @Override public boolean autoUnlockSex() { return false; }
    @Override public NpcHandModel createHandModel(int slot) { return new SlimeHandModel(); }
    @Override public String getHandTexturePath(int slot) { return "textures/entity/slime/hand.png"; }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        if ("action.names.blowjob".equals(action)) {
            setSubAnimState(0, AnimState.SUCKBLOWJOB);
            setAnimStateFiltered(AnimState.SUCKBLOWJOB);
            setPartnerUUID(playerId);
        }
    }

    @Override
    public boolean onPlayerInteract(net.minecraft.world.entity.player.Player player) {
        openActionMenu(player, this, new String[]{ "action.names.blowjob" }, false);
        return true;
    }

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        if (cur == AnimState.CUMBLOWJOB && (next == AnimState.THRUSTBLOWJOB || next == AnimState.SUCKBLOWJOB)) return;
        if (cur == AnimState.DOGGYCUM   && (next == AnimState.DOGGYFAST     || next == AnimState.DOGGYSLOW))   return;
        super.setAnimStateFiltered(next);
    }

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.SUCKBLOWJOB) return AnimState.THRUSTBLOWJOB;
        if (cur == AnimState.DOGGYSLOW)   return AnimState.DOGGYFAST;
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.SUCKBLOWJOB || cur == AnimState.THRUSTBLOWJOB) return AnimState.CUMBLOWJOB;
        if (cur == AnimState.DOGGYSLOW   || cur == AnimState.DOGGYFAST)     return AnimState.DOGGYCUM;
        return null;
    }

    @Override
    public void tick() {
        super.tick();
        if (getAnimState() != AnimState.WAITDOGGY) return;
        var target = getTargetPlayer();
        if (target == null || target.position().distanceToSqr(getSexOffsetPos()) > 1.0) return;
        if (!level.isClientSide()) {
            if (target instanceof net.minecraft.server.level.ServerPlayer sp)
                ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new CameraControlPacket(false));
            setPartnerUUID(target.getUUID());
            target.setYRot(getStoredYaw());
            target.setPos(getSexOffsetPos().x, getSexOffsetPos().y, getSexOffsetPos().z);
            target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            setAnimStateFiltered(AnimState.DOGGYSTART);
            target.setNoGravity(true);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes",     5, s -> { s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null) ? RawAnimation.begin().thenLoop("animation.slime.fhappy") : RawAnimation.begin().thenLoop("animation.slime.null")); return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<SlimePlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.null")); return PlayState.CONTINUE; }
        if (a == AnimState.SIT)          s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.sit"));
        else if (isFallFlying)           s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.fly" + (flyAlt ? "2" : "")));
        else if (s.isMoving())           s.setAndContinue(RawAnimation.begin().thenLoop(getDeltaMovement().horizontalDistanceSqr() > 0.04 ? "animation.slime.run" : "animation.slime.walk"));
        else                             s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<SlimePlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case UNDRESS       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.undress"));
            case DRESS         -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.dress"));
            case STRIP         -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.strip"));
            case SUCKBLOWJOB   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.blowjobsuck"));
            case THRUSTBLOWJOB -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.blowjobthrust"));
            case CUMBLOWJOB    -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.blowjobcum"));
            case DOGGYGOONBED  -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggygoonbed"));
            case WAITDOGGY     -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggywait"));
            case DOGGYSTART    -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggystart"));
            case DOGGYSLOW     -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggyslow"));
            case DOGGYFAST     -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggyfast"));
            case DOGGYCUM      -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggycum"));
            case ATTACK        -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.attack" + attackCounter));
            case BOW_CHARGE    -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.bowcharge"));
            case RIDE          -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.ride"));
            case SIT           -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.sit"));
            default            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
