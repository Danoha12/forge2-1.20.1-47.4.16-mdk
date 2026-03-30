package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.model.entity.GalathHandModel;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * GalathPlayerKobold — Portado a 1.20.1 / GeckoLib 4.
 * * Avatar del jugador para la especie Galath.
 * * Implementa vuelo controlado y las cadenas de interacción de la súcubo.
 * * Escala masiva de 2.3.
 */
public class GalathPlayerKobold extends PlayerKoboldEntity implements FlyingNpcEntity {

    public boolean flyAlt = false;
    public int ar = 0;       // Sufijo para variante de RAPE_ON_GOING (0, 1, 2...)
    public boolean as = false; // Variante de CORRUPT: true = hard, false = soft
    public boolean aq = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public GalathPlayerKobold(Level level) {
        super(level);
    }

    public GalathPlayerKobold(Level level, UUID uuid) {
        super(level, uuid);
    }

    // ── Propiedades de Modelo ────────────────────────────────────────────────

    @Override public float getModelScale() { return 2.3F; }

    @Override public NpcHandModel createHandModel(int slot) { return new GalathHandModel(); }

    @Override public String getHandTexturePath(int slot) { return "textures/entity/galath/hand.png"; }

    // ── IA y Selección de Acciones ───────────────────────────────────────────

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "cowgirl" -> {
                setPartnerUUID(playerId);
                setAnimStateFiltered(AnimState.RAPE_INTRO);
                // Sincroniza los brazos del avatar
                setSubAnimState(getArmHeightSlot(), AnimState.RAPE_INTRO);
            }
            case "mating press" -> {
                setPartnerUUID(playerId);
                setAnimStateFiltered(AnimState.CORRUPT_SLOW);
                setSubAnimState(getArmHeightSlot(), AnimState.CORRUPT_SLOW);
                startFlight(); // Inicia el vuelo de combate/interacción
            }
        }
    }

    // ── Control de Estados (Protección de Cadenas) ───────────────────────────

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        // Evita que la animación se reinicie si el jugador intenta "regresar" desde un CUM state
        if (cur == AnimState.CORRUPT_CUM && (next == AnimState.CORRUPT_FAST || next == AnimState.CORRUPT_SLOW)) return;
        if (cur == AnimState.RAPE_CUM && next == AnimState.RAPE_ON_GOING) return;
        super.setAnimStateFiltered(next);
    }

    @Override
    public AnimState getNextState(AnimState cur) {
        // CORRUPT_SLOW escala automáticamente a FAST si se mantienen las condiciones
        if (cur == AnimState.CORRUPT_SLOW) return AnimState.CORRUPT_FAST;
        return null;
    }

    @Override
    public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.CORRUPT_SLOW || cur == AnimState.CORRUPT_FAST) return AnimState.CORRUPT_CUM;
        if (cur == AnimState.RAPE_ON_GOING) return AnimState.RAPE_CUM;
        return null;
    }

    public void startFlight() {
        // La lógica real de propulsión la maneja el FlightController en el cliente/servidor
    }

    // ── GeckoLib 4 Controllers ────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Ojos y Parpadeo
        registrar.add(new AnimationController<>(this, "eyes", 5, state -> {
            AnimState current = getAnimState();
            String anim = (current == AnimState.NULL || current == null) ? "animation.galath.blink" : "animation.galath.null";
            return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
        }));

        // Movimiento (Caminar, Correr, Agacharse, Volar)
        registrar.add(new AnimationController<>(this, "movement", 5, this::movementController));

        // Acciones Especiales e Interacciones
        registrar.add(new AnimationController<>(this, "action", 0, this::actionController));
    }

    private PlayState movementController(AnimationState<GalathPlayerKobold> state) {
        AnimState a = getAnimState();
        // Si hay una acción prioritaria, no reproducir animaciones de caminata
        if (a != AnimState.NULL && a != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.null"));
        }

        if (a == AnimState.SIT) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.sit"));
        } else if (this.isFallFlying) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.controlled_flight"));
        } else if (state.isMoving()) {
            double speed = this.getDeltaMovement().horizontalDistanceSqr();
            if (this.isCrouching()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.crouchwalk"));
            }
            String moveAnim = speed > 0.04 ? "animation.galath.run" : "animation.galath.walk";
            return state.setAndContinue(RawAnimation.begin().thenLoop(moveAnim));
        } else {
            // Idle
            String idleAnim = this.isCrouching() ? "animation.galath.crouchidle" : "animation.galath.idle";
            return state.setAndContinue(RawAnimation.begin().thenLoop(idleAnim));
        }
    }

    private PlayState actionController(AnimationState<GalathPlayerKobold> state) {
        AnimState a = getAnimState();
        if (a == null) return PlayState.CONTINUE;

        return switch (a) {
            case STRIP         -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.strip"));
            case ATTACK        -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.galath.attack1"));
            case BOW_CHARGE    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.bowcharge"));
            case SIT           -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.sit"));
            case RAPE_INTRO    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape_intro"));
            case RAPE_ON_GOING -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape" + ar));
            case RAPE_CUM      -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape_cum"));
            case RAPE_CUM_IDLE -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.rape_cum_idle"));
            case CORRUPT_INTRO -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.galath.corrupt_intro"));
            case CORRUPT_SLOW  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.corrupt_slow"));
            case CORRUPT_FAST  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.corrupt_" + (as ? "hard" : "soft")));
            case CORRUPT_CUM   -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.galath.corrupt_cum"));
            case FLY           -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.galath.controlled_flight"));
            default            -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.galath.null"));
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}