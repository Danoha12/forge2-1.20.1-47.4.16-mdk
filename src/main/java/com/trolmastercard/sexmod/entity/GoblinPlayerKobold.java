package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.model.entity.GoblinHandModel;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * GoblinPlayerKobold — Portado a 1.20.1 / GeckoLib 4.
 * * Entidad espejo (Avatar) para el jugador cuando asume el rol o interactúa con el Goblin.
 * * Mapea las cadenas de animaciones de interacción (Carry, Nelson, Breeding, Paizuri).
 */
public class GoblinPlayerKobold extends PlayerKoboldEntity implements NpcInventoryCapable {

    public boolean isQueen = false;
    public int attackCounter = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public GoblinPlayerKobold(Level level) {
        super(level);
    }

    public GoblinPlayerKobold(Level level, UUID uuid) {
        super(level, uuid);
    }

    // ── Modelos de Manos (Primera Persona) ────────────────────────────────────

    @Override
    public NpcHandModel createHandModel(int slot) {
        return new GoblinHandModel();
    }

    @Override
    public String getHandTexturePath(int slot) {
        return "textures/entity/goblin/hand.png";
    }

    // ── Cadenas de Animación (Speed-up & Cum) ─────────────────────────────────

    @Override
    public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.NELSON_SLOW)  return AnimState.NELSON_FAST;
        if (cur == AnimState.PAIZURI_SLOW) return AnimState.PAIZURI_FAST;
        return null;
    }

    @Override
    public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.NELSON_SLOW || cur == AnimState.NELSON_FAST) return AnimState.NELSON_CUM;
        if (cur == AnimState.PAIZURI_SLOW || cur == AnimState.PAIZURI_FAST) return AnimState.PAIZURI_CUM;
        if (cur == AnimState.BREEDING_SLOW || cur == AnimState.BREEDING_FAST) return AnimState.BREEDING_CUM;
        return null;
    }

    // ── GeckoLib 4 Controllers ────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Asumo que tienes este método en la clase base, si no, lo puedes quitar
        // ensureControllerInit();

        registrar.add(
                new AnimationController<>(this, "eyes", 5, this::eyesController),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action", 0, this::actionController)
        );
    }

    private PlayState eyesController(AnimationState<GoblinPlayerKobold> state) {
        AnimState current = getAnimState();
        if (current == AnimState.NULL || current == null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.blink"));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.null"));
    }

    private PlayState movementController(AnimationState<GoblinPlayerKobold> state) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.null"));
        }

        if (a == AnimState.SIT) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.sit"));
        } else if (this.isFallFlying) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.fly"));
        } else if (state.isMoving()) {
            // Distinción entre caminar y correr basada en la velocidad horizontal
            String anim = this.getDeltaMovement().horizontalDistanceSqr() > 0.04 ? "animation.goblin.running" : "animation.goblin.walk";
            return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
        } else {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.idle"));
        }
    }

    private PlayState actionController(AnimationState<GoblinPlayerKobold> state) {
        AnimState a = getAnimState();
        if (a == null) return PlayState.CONTINUE;

        // Determinar vista de cámara (solo en el cliente) para animaciones que tapan la pantalla
        String view = "thirdperson";
        if (FMLEnvironment.dist == Dist.CLIENT) {
            if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
                view = "firstperson";
            }
        }

        return switch (a) {
            case SHOULDER_IDLE  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.shoulder_idle"));
            case PICK_UP        -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.pick_up_" + view));
            case START_THROWING -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.throw_" + view));
            case THROWN         -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.thrown"));
            case STAND_UP       -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.stand_up"));
            case STRIP          -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.strip"));
            case ATTACK         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.attack"));
            case BOW_CHARGE     -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.bowcharge"));

            // Cadenas de Escenas
            case NELSON_INTRO   -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.nelson_intro"));
            case NELSON_SLOW    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.nelson_slow"));
            case NELSON_FAST    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.nelson_fast"));
            case NELSON_CUM     -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.nelson_cum"));

            // Variantes de Reina vs Normal
            case BREEDING_INTRO -> state.setAndContinue(RawAnimation.begin().thenPlay(isQueen ? "animation.goblin.breeding_intro_2" : "animation.goblin.breeding_intro_1"));
            case BREEDING_SLOW  -> state.setAndContinue(RawAnimation.begin().thenLoop(isQueen ? "animation.goblin.breeding_slow_3" : "animation.goblin.breeding_slow_1"));
            case BREEDING_FAST  -> state.setAndContinue(RawAnimation.begin().thenLoop(isQueen ? "animation.goblin.breeding_fast_3" : "animation.goblin.breeding_fast_1"));
            case BREEDING_CUM   -> state.setAndContinue(RawAnimation.begin().thenPlay(isQueen ? "animation.goblin.breeding_cum_2" : "animation.goblin.breeding_cum_1"));
            case BREEDING_LOOP  -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.breeding_2"));

            case PAIZURI_START  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.paizuri_start"));
            case PAIZURI_SLOW   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_slow"));
            case PAIZURI_FAST   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_fast"));
            case PAIZURI_IDLE   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_idle"));
            case PAIZURI_CUM    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.paizuri_cum"));

            default -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.null"));
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}