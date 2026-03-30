package com.trolmastercard.sexmod.entity.player;

import com.trolmastercard.sexmod.client.model.EllieHandModel;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.NpcActionQueuePacket;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * ElliePlayerKobold — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * Define el comportamiento de un Kobold que ha tomado la forma de Ellie.
 * - Escala: 2.05x (Gigante).
 * - Altura de ojos dinámica.
 * - Progresión de estados: Slow -> Fast -> Finish.
 */
public class ElliePlayerKobold extends PlayerKoboldEntity {

    private int carrySlowVariant = 1;
    private boolean alternateFly = false;
    private boolean actionSent = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public ElliePlayerKobold(Level level) { super(level); }
    public ElliePlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 2.05F; }

    @Override
    public float getEyeHeight(Pose pose) {
        return this.isCrouching() ? 1.53F : 1.9F;
    }

    // ── Configuración de Brazos/Manos ─────────────────────────────────────────

    @Override
    public EllieHandModel createHandModel() { return new EllieHandModel(); }

    @Override
    public String getHandTexturePath(int slot) {
        // Slot 0: Piel natural | Slot 1: Con vestimenta
        return slot == 0 ? "textures/entity/ellie/hand_nude.png" : "textures/entity/ellie/hand.png";
    }

    // ── Lógica de Interacciones ───────────────────────────────────────────────

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "action.names.sitting_ride" -> queueFollowUp("SittingRide");
            case "action.names.front_dance" -> queueFollowUp("FrontDance");
            case "action.names.lift_dance" -> {
                setPartnerUUID(playerId);
                setAnimStateFiltered(AnimState.LIFT_DANCE_START);
            }
        }

        // Sincronizar acción con el servidor
        if (getOwnerUUID() != null) {
            ModNetwork.CHANNEL.sendToServer(new NpcActionQueuePacket(action, playerId, getOwnerUUID(), actionSent));
            actionSent = true;
        }
    }

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();

        // Bloqueo de estados si ya se ha llegado al final (Finish/Cum)
        if (cur == AnimState.FRONT_DANCE_FINISH && (next == AnimState.FRONT_DANCE_FAST || next == AnimState.FRONT_DANCE_SLOW)) return;
        if (cur == AnimState.SITTING_RIDE_FINISH && (next == AnimState.SITTING_RIDE_FAST || next == AnimState.SITTING_RIDE_SLOW)) return;
        if (cur == AnimState.LIFT_DANCE_FINISH && (next == AnimState.LIFT_DANCE_FAST || next == AnimState.LIFT_DANCE_SLOW)) return;

        super.setAnimStateFiltered(next);
    }

    // ── Máquina de Estados (Progresión) ───────────────────────────────────────

    @Override
    public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.SITTING_RIDE_SLOW) return AnimState.SITTING_RIDE_FAST;
        if (cur == AnimState.FRONT_DANCE_SLOW) return AnimState.FRONT_DANCE_FAST;
        if (cur == AnimState.LIFT_DANCE_SLOW) return AnimState.LIFT_DANCE_FAST;
        return null;
    }

    @Override
    public AnimState getFinishState(AnimState cur) {
        if (cur == AnimState.SITTING_RIDE_SLOW || cur == AnimState.SITTING_RIDE_FAST) return AnimState.SITTING_RIDE_FINISH;
        if (cur == AnimState.FRONT_DANCE_SLOW || cur == AnimState.FRONT_DANCE_FAST) return AnimState.FRONT_DANCE_FINISH;
        if (cur == AnimState.LIFT_DANCE_SLOW || cur == AnimState.LIFT_DANCE_FAST) return AnimState.LIFT_DANCE_FINISH;
        return null;
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "eyes", 5, state -> {
                    AnimState as = getAnimState();
                    return state.setAndContinue(as == AnimState.NULL || as == null
                            ? RawAnimation.begin().thenLoop("animation.ellie.eyes")
                            : RawAnimation.begin().thenLoop("animation.ellie.null"));
                }),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action", 0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<ElliePlayerKobold> state) {
        AnimState as = getAnimState();
        if (as != AnimState.NULL && as != null) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.null"));

        if (this.isFallFlying()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.fly" + (alternateFly ? "2" : "")));
        }

        if (state.isMoving()) {
            String moveAnim = this.isCrouching() ? "animation.ellie.crouchwalk" : "animation.ellie.run";
            return state.setAndContinue(RawAnimation.begin().thenLoop(moveAnim));
        }

        return state.setAndContinue(RawAnimation.begin().thenLoop(this.isCrouching() ? "animation.ellie.crouchidle" : "animation.ellie.idle"));
    }

    private PlayState actionController(AnimationState<ElliePlayerKobold> state) {
        AnimState as = getAnimState();
        if (as == null) return PlayState.CONTINUE;

        return switch (as) {
            case SITDOWNIDLE      -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.sitdownidle"));
            case SITTING_RIDE_SLOW -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlslow2"));
            case SITTING_RIDE_FAST -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlfast"));
            case SITTING_RIDE_FINISH -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.cowgirlcum"));

            case FRONT_DANCE_START -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.missionary_start"));
            case FRONT_DANCE_SLOW -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.missionary_slow"));
            case FRONT_DANCE_FINISH -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.missionary_cum"));

            case LIFT_DANCE_START -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.ellie.carry_intro"));
            case LIFT_DANCE_SLOW -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.carry_slow" + carrySlowVariant));
            case LIFT_DANCE_FINISH -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.carry_cum"));

            default -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.null"));
        };
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}