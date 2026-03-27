package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcHandModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * AlliePlayerKobold - ported from e5.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Deepthroat chain:       DEEPTHROAT_START - DEEPTHROAT_SLOW - DEEPTHROAT_FAST - DEEPTHROAT_CUM
 * Reverse cowgirl chain:  REVERSE_COWGIRL_START - SLOW(variant ar) - FAST(variant av) - CUM
 * Also: SUMMON, SUMMON_NORMAL, SUMMON_WAIT, RICH, ATTACK, BOW_CHARGE.
 *
 * Cannot fly (v()=false). Scale 1.9+scaleAdjust. Eye height 1.63.
 * Hand model: AllieHandModel (f1). Hand texture: textures/entity/allie/hand.png.
 *
 * Particle effect: emits PORTAL particles (radius 4) while in DEEPTHROAT_FAST state.
 * ar, av fields = sub-variant counters for reverse_cowgirl_slow/fastc animations.
 */
public class AlliePlayerKobold extends PlayerKoboldEntity {

    public float scaleAdjust = 0.0F;
    Player targetPlayer = null;
    boolean invoked     = false;
    int attackCounter   = 1;
    int ar = 1; // reverse_cowgirl_slow variant suffix
    int av = 1; // reverse_cowgirl_fastc variant suffix

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public AlliePlayerKobold(Level level) { super(level); }
    public AlliePlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.9F + scaleAdjust; }
    @Override public float getEyeHeight(net.minecraft.world.entity.Pose p) { return 1.63F; }
    @Override public boolean canFly() { return false; }
    @Override public NpcHandModel createHandModel(int slot) { return new AllieHandModel(); }
    @Override public String getHandTexturePath(int slot) { return "textures/entity/allie/hand.png"; }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "action.names.deepthroat" -> {
                setAnimStateFiltered(AnimState.DEEPTHROAT_START);
                setSubAnimState(getArmHeightSlot(), AnimState.DEEPTHROAT_START);
                setPartnerUUID(playerId);
            }
            case "Reverse cowgirl" -> {
                setAnimStateFiltered(AnimState.REVERSE_COWGIRL_START);
                setSubAnimState(0, AnimState.REVERSE_COWGIRL_START);
                setPartnerUUID(playerId);
            }
        }
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        openActionMenu(player, this, new String[]{ "action.names.deepthroat" }, false);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // Portal particles during deepthroat_fast
        if (level.isClientSide() && getAnimState() == AnimState.DEEPTHROAT_FAST) {
            double radius = 4.0;
            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                level.addParticle(ParticleTypes.PORTAL,
                        getX() + Math.cos(angle) * radius * random.nextDouble(),
                        getY() + random.nextDouble() * radius,
                        getZ() + Math.sin(angle) * radius * random.nextDouble(),
                        0, 0, 0);
            }
        }
    }

    @Override public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.DEEPTHROAT_SLOW)       return AnimState.DEEPTHROAT_FAST;
        if (cur == AnimState.REVERSE_COWGIRL_SLOW)  return AnimState.REVERSE_COWGIRL_FASTC;
        if (cur == AnimState.REVERSE_COWGIRL_FASTC) return AnimState.REVERSE_COWGIRL_FASTS;
        return null;
    }

    @Override public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.DEEPTHROAT_SLOW || cur == AnimState.DEEPTHROAT_FAST)      return AnimState.DEEPTHROAT_CUM;
        if (cur == AnimState.REVERSE_COWGIRL_SLOW || cur == AnimState.REVERSE_COWGIRL_FASTC || cur == AnimState.REVERSE_COWGIRL_FASTS) return AnimState.REVERSE_COWGIRL_CUM;
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
            new AnimationController<>(this, "eyes",     5, s -> { s.setAndContinue((getAnimState()==AnimState.NULL||getAnimState()==null) ? RawAnimation.begin().thenLoop("animation.bia.blink") : RawAnimation.begin().thenLoop("animation.allie.null")); return PlayState.CONTINUE; }),
            new AnimationController<>(this, "tail",     5, s -> { s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.tail")); return PlayState.CONTINUE; }),
            new AnimationController<>(this, "movement", 5, this::movementController),
            new AnimationController<>(this, "action",   0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<AlliePlayerKobold> s) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) { s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null")); return PlayState.CONTINUE; }
        if (s.isMoving()) s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
        else              s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<AlliePlayerKobold> s) {
        AnimState a = getAnimState(); if (a == null) return PlayState.CONTINUE;
        switch (a) {
            case SUMMON                 -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon"));
            case SUMMON_NORMAL          -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon_normal"));
            case SUMMON_NORMAL_WAIT     -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.summon_normal_wait"));
            case SUMMON_WAIT            -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.summon_wait"));
            case DEEPTHROAT_PREPARE     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_prepare"));
            case DEEPTHROAT_NORMAL_PREP -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_normal_prepare"));
            case DEEPTHROAT_START       -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_start"));
            case DEEPTHROAT_SLOW        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.deepthroat_slow"));
            case DEEPTHROAT_FAST        -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.deepthroat_fast"));
            case DEEPTHROAT_CUM         -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_cum"));
            case RICH                   -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.rich"));
            case RICH_NORMAL            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.rich_normal"));
            case SUMMON_SAND            -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon_sand"));
            case ATTACK                 -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.attack" + attackCounter));
            case BOW_CHARGE             -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.bowcharge"));
            case REVERSE_COWGIRL_START  -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_start"));
            case REVERSE_COWGIRL_SLOW   -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_slow" + ar));
            case REVERSE_COWGIRL_FASTC  -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_fastc" + av));
            case REVERSE_COWGIRL_FASTS  -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_fasts"));
            case REVERSE_COWGIRL_CUM    -> s.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_cum"));
            default                     -> s.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.null"));
        }
        return PlayState.CONTINUE;
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
