package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * JennyPlayerKobold - ported from es.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Blowjob chain:  STARTBLOWJOB - SUCKBLOWJOB - THRUSTBLOWJOB - CUMBLOWJOB
 * Doggy chain:    DOGGYGOONBED - WAITDOGGY - DOGGYSTART - DOGGYSLOW - DOGGYFAST - DOGGYCUM
 * Paizuri chain:  PAIZURI_START - PAIZURI_SLOW - PAIZURI_FAST - PAIZURI_CUM
 * WAITDOGGY: checks lesbo flag (ar), grabs player when close.
 * Scale 1.75. Eye height 1.64. autoUnlockSex = false.
 * ap = flyAlt. ar = doggyFast hard variant bool. as = paizuri cam flag.
 */
public class JennyPlayerKobold extends PlayerKoboldEntity {

    boolean flyAlt = false;
    boolean doggyHard = false; // ar  doggyfast_hard vs doggyfast_soft
    int attackCounter = 1;
    boolean paiCamFlag = false; // as
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public JennyPlayerKobold(Level level) { super(level); }
    public JennyPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.75F; }
    @Override public float getEyeHeight(net.minecraft.world.entity.Pose p) { return 1.64F; }
    @Override public boolean autoUnlockSex() { return false; }
    @Override public NpcHandModel createHandModel(int slot) { return slot == 0 ? new JennyHandNudeModel() : new JennyHandModel(); }
    @Override public String getHandTexturePath(int slot) { return slot == 0 ? "textures/entity/jenny/hand_nude.png" : "textures/entity/jenny/hand.png"; }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "action.names.boobjob" -> {
                entityData.set(BaseNpcEntity.MODEL_INDEX, 0);
                setAnimStateFiltered(AnimState.PAIZURI_START);
                setSubAnimState(0, AnimState.PAIZURI_START);
                setPartnerUUID(playerId);
            }
            case "action.names.blowjob" -> {
                setAnimStateFiltered(AnimState.STARTBLOWJOB);
                setSubAnimState(getArmHeightSlot(), AnimState.PAIZURI_START);
                setPartnerUUID(playerId);
            }
        }
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        openActionMenu(player, this, new String[]{ "action.names.blowjob", "action.names.boobjob" }, false);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        if (getAnimState() != AnimState.WAITDOGGY) return;
        var target = getTargetPlayer();
        if (target == null) return;
        if (target.position().distanceToSqr(getSexOffsetPos()) >= 1.0) return;

        if (!level.isClientSide()) {
            // Lesbo check
            if (isPartnerFemale(target)) {
                target.sendSystemMessage(Component.literal(ChatFormatting.DARK_PURPLE + "sowy no lesbo action yet uwu"));
                return;
            }
            setPartnerUUID(target.getUUID());
            target.setPos(getX(), getSexOffsetPos().y, getZ());
            stopPlayerMovement((net.minecraft.server.level.ServerPlayer) target, false);
            target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            target.getAbilities().flying = true;
            getMaster().ifPresent(m -> level.getPlayerByUUID(m) != null && (level.getPlayerByUUID(m)).getAbilities().flying == true);
            setDeltaMovement(0, 0, 0.4);
            setAnimStateFiltered(AnimState.DOGGYSTART);
            ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() ->
                    (net.minecraft.server.level.ServerPlayer) target), new CameraControlPacket(false));
        }
    }

    private boolean isPartnerFemale(Player p) { return false; } // placeholder

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.SUCKBLOWJOB)  return AnimState.THRUSTBLOWJOB;
        if (cur == AnimState.DOGGYSLOW)    return AnimState.DOGGYFAST;
        if (cur == AnimState.PAIZURI_SLOW) { if (paiCamFlag) { paiCamFlag = false; } return AnimState.PAIZURI_FAST; }
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.SUCKBLOWJOB || cur == AnimState.THRUSTBLOWJOB) return AnimState.CUMBLOWJOB;
        if (cur == AnimState.DOGGYSLOW   || cur == AnimState.DOGGYFAST)     return AnimState.DOGGYCUM;
        if (cur == AnimState.PAIZURI_SLOW|| cur == AnimState.PAIZURI_FAST)  return AnimState.PAIZURI_CUM;
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes", 5, s -> { s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null) ? RawAnimation.begin().thenLoop("animation.jenny.fhappy") : RawAnimation.begin().thenLoop("animation.jenny.null")); return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<JennyPlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.null")); return PlayState.CONTINUE; }
        if (a == AnimState.SIT) s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.sit"));
        else if (isFallFlying)  s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.fly" + (flyAlt ? "2" : "")));
        else if (s.isMoving())  s.setAndContinue(RawAnimation.begin().thenLoop(getDeltaMovement().horizontalDistanceSqr()>0.04 ? "animation.jenny.run" : "animation.jenny.fastwalk"));
        else                    s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.idle"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<JennyPlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case STRIP          -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.strip"));
            case PAYMENT        -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.payment"));
            case STARTBLOWJOB   -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.blowjobintro"));
            case SUCKBLOWJOB    -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.blowjobsuck"));
            case THRUSTBLOWJOB  -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.blowjobthrust"));
            case CUMBLOWJOB     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.blowjobcum"));
            case DOGGYGOONBED   -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.doggygoonbed"));
            case WAITDOGGY      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.doggywait"));
            case DOGGYSTART     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.doggystart"));
            case DOGGYSLOW      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.doggyslow"));
            case DOGGYFAST      -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.doggyfast_" + (doggyHard ? "hard" : "soft")));
            case DOGGYCUM       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.doggycum"));
            case ATTACK         -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.attack" + attackCounter));
            case BOW_CHARGE     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.bowcharge"));
            case RIDE           -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.ride"));
            case SIT            -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.sit"));
            case THROW_PEARL    -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.throwpearl"));
            case DOWNED         -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.downed"));
            case PAIZURI_START  -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.paizuri_start"));
            case PAIZURI_SLOW   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.paizuri_slow"));
            case PAIZURI_FAST   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.paizuri_fast"));
            case PAIZURI_CUM    -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.paizuri_cum"));
            default             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.jenny.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
