package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

import static com.trolmastercard.sexmod.registry.AnimState.*;

/**
 * BiaPlayerKobold — Portado a 1.20.1.
 * Código corregido: Switch expressions modernos y limpieza de controladores.
 */
public class BiaPlayerKobold extends PlayerKoboldEntity {

    private boolean flyAlt = false;
    private int aq = 1; // Sufijo para variante PRONE_DANCE_HARD
    private int attackCounter = 1;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BiaPlayerKobold(Level level) { super(level); }
    public BiaPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.5F; }
    @Override public float getEyeHeight(Pose pose) { return 1.5F; }

    // ── Lógica de Interacción ──────────────────────────────────────────────────

    public boolean onInteractiveActionRequest(String action) {
        switch (action) {
            case "back" -> {
                setAnimStateFiltered(ANAL_PREPARE);
                return true;
            }
            case "dance" -> {
                setAnimStateFiltered(SITDOWN);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        openActionMenu(player, this, new String[]{ "back", "dance", "action.names.headpat" }, false);
        return true;
    }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        if ("action.names.headpat".equals(action)) {
            setPartnerUUID(playerId);
            setAnimStateFiltered(HEAD_PAT);
        }
    }

    @Override
    public AnimState getNextState(AnimState current) {
        if (current == ANAL_SLOW) return ANAL_FAST;
        if (current == PRONE_DOGGY_SOFT) return PRONE_DOGGY_HARD;
        return null;
    }

    @Override
    public AnimState getCumState(AnimState current) {
        if (current == ANAL_SLOW || current == ANAL_FAST) return ANAL_CUM;
        if (current == PRONE_DOGGY_SOFT || current == PRONE_DOGGY_HARD) return PRONE_DOGGY_CUM;
        return null;
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "eyes", 5, state -> {
                    AnimState as = getAnimState();
                    if (as == NULL || as == null) {
                        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.fhappy"));
                    }
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.null"));
                }),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action", 0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<BiaPlayerKobold> state) {
        AnimState current = getAnimState();
        if (current != NULL && current != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.null"));
        }

        if (current == SIT) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sit"));

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
        AnimState current = getAnimState();
        if (current == null) return PlayState.CONTINUE;

        // 🚀 Switch Expression: Devuelve el RawAnimation directamente
        return switch (current) {
            case STRIP          -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.strip"));
            case ATTACK         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.attack" + attackCounter));
            case BOW            -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.bowcharge"));
            case RIDE           -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.ride"));
            case SIT            -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sit"));
            case THROW_PEARL    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.throwpearl"));
            case DOWNED         -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.downed"));

            case TALK_HORNY     -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.talk_horny"));
            case TALK_IDLE      -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.talk_idle"));
            case TALK_RESPONSE  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.talk_response"));

            // Secuencia ANAL (BACK)
            case ANAL_PREPARE   -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.anal_prepare"));
            case ANAL_WAIT      -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_wait"));
            case ANAL_START     -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_start"));
            case ANAL_SLOW      -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_slow"));
            case ANAL_FAST      -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.anal_fast"));
            case ANAL_CUM       -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.anal_cum"));

            case HEAD_PAT       -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.headpat"));
            case SITDOWN        -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.sitdown"));
            case SITDOWNIDLE    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.sitdownidle"));

            // Secuencia PRONE_DOGGY (DANCE)
            case PRONE_DOGGY_INTRO  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_intro"));
            case PRONE_DOGGY_INSERT -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_insert"));
            case PRONE_DOGGY_SOFT   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_soft"));
            case PRONE_DOGGY_HARD   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_hard" + aq));
            case PRONE_DOGGY_CUM    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.prone_doggy_cum"));

            default             -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bia.null"));
        };
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}