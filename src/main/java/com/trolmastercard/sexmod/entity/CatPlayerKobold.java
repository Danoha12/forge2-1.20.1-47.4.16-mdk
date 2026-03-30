package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.model.entity.CatHandModel;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * CatPlayerKobold — Portado a 1.20.1.
 * * Variante de avatar de jugador para "Cat" (Luna).
 * * Implementa lógica de pesca, escalas personalizadas (1.6x) y secuencias de animación.
 */
public class CatPlayerKobold extends PlayerKoboldEntity {

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    protected boolean flyAlt = false;
    protected boolean slowVariant = false; // Antiguo campo 'ap'
    protected int attackCounter = 1;

    public CatPlayerKobold(Level level, UUID uuid) {
        super(ModEntities.PLAYER_CAT.get(), level);
        this.setOwnerUUID(uuid);
    }

    // ── Atributos Físicos ────────────────────────────────────────────────────

    @Override
    public float getModelScale() {
        return 1.6F;
    }

    @Override
    public float getEyeHeight(Pose pose, EntityDimensions dimensions) {
        // Altura de ojos calibrada para el modelo de 1.6x
        return 1.34F;
    }

    // ── Renderizado de Manos ─────────────────────────────────────────────────

    // Asumiendo que definiste NpcHandModel en tu estructura
    /*
    @Override
    public NpcHandModel createHandModel() {
        return new CatHandModel();
    }
    */

    @Override
    public String getHandTexturePath() {
        return "sexmod:textures/entity/cat/hand.png";
    }

    // ── Lógica de Interacción ────────────────────────────────────────────────

    @Override
    public void onActionSelected(String action, UUID playerId) {
        if ("action.names.touchboobs".equals(action)) {
            this.setSubAnimState(0, AnimState.TOUCH_BOOBS_INTRO);
            this.setAnimStateFiltered(AnimState.TOUCH_BOOBS_INTRO);
            // TARGET_ID_PARAM suele ser el DataParameter para saber quién es el partner
            // this.getEntityData().set(TARGET_ID_PARAM, 0);
            this.startInteraction(playerId);
        }
        super.onActionSelected(action, playerId);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isOwnedByLocalPlayer()) {
            // Abrir el menú de acciones para uno mismo (si el mod lo permite)
            // openActionMenu(player, this, new String[]{ "action.names.touchboobs" }, false);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // ── Gestión de Secuencias (Máquina de Estados) ───────────────────────────

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        // Evitar "bajar de intensidad" una vez que llegamos al final (CUM)
        if (cur == AnimState.TOUCH_BOOBS_CUM && (next == AnimState.TOUCH_BOOBS_FAST || next == AnimState.TOUCH_BOOBS_SLOW)) return;
        if (cur == AnimState.COWGIRL_SITTING_CUM && (next == AnimState.COWGIRL_SITTING_FAST || next == AnimState.COWGIRL_SITTING_SLOW)) return;

        super.setAnimStateFiltered(next);
    }

    @Override
    public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.TOUCH_BOOBS_SLOW) return AnimState.TOUCH_BOOBS_FAST;
        if (cur == AnimState.COWGIRL_SITTING_SLOW) return AnimState.COWGIRL_SITTING_FAST;
        return super.getNextState(cur);
    }

    @Override
    public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.TOUCH_BOOBS_SLOW || cur == AnimState.TOUCH_BOOBS_FAST) return AnimState.TOUCH_BOOBS_CUM;
        if (cur == AnimState.COWGIRL_SITTING_SLOW || cur == AnimState.COWGIRL_SITTING_FAST) return AnimState.COWGIRL_SITTING_CUM;
        return super.getCumState(cur);
    }

    // ── Animaciones (GeckoLib 4) ─────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "eyes", 5, state -> {
                    String anim = (getAnimState() == AnimState.NULL || getAnimState() == null) ? "animation.cat.blink" : "animation.cat.null";
                    return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
                }),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action", 0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<CatPlayerKobold> state) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.null"));
        }

        if (a == AnimState.SIT) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.sit"));

        // Lógica de vuelo (Elytra o transformación)
        // if (this.isFallFlying()) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.fly" + (flyAlt ? "2" : "")));

        if (state.isMoving()) {
            String walkAnim = getDeltaMovement().horizontalDistanceSqr() > 0.04 ? "animation.cat.run" : "animation.cat.fastwalk";
            return state.setAndContinue(RawAnimation.begin().thenLoop(walkAnim));
        }

        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.cat.idle"));
    }

    private PlayState actionController(AnimationState<CatPlayerKobold> state) {
        AnimState a = getAnimState();
        if (a == null) return PlayState.STOP;

        return state.setAndContinue(RawAnimation.begin().thenLoop(switch (a) {
            case ATTACK -> "animation.cat.attack" + attackCounter;
            case SIT -> "animation.cat.sit";
            case BOW_CHARGE -> "animation.cat.bowcharge";
            case THROW_PEARL -> "animation.cat.throwpearl";
            case DOWNED -> "animation.cat.downed";
            case FISHING_START -> "animation.cat.start_fishing";
            case FISHING_IDLE -> "animation.cat.idle_fishing";
            case FISHING_EAT -> "animation.cat.eat_fishing";
            case THROW_AWAY -> "animation.cat.throw_away";
            case PAYMENT -> "animation.cat.payment";
            case TOUCH_BOOBS_INTRO -> "animation.cat.touch_boobs_intro";
            case TOUCH_BOOBS_SLOW -> "animation.cat.touch_boobs_slow" + (slowVariant ? "1" : "");
            case TOUCH_BOOBS_FAST -> "animation.cat.touch_boobs_fast";
            case TOUCH_BOOBS_CUM -> "animation.cat.touch_boobs_cum";
            case WAIT_CAT -> "animation.cat.wait";
            case COWGIRL_SITTING_INTRO -> "animation.cat.sitting_intro";
            case COWGIRL_SITTING_SLOW -> "animation.cat.sitting_slow";
            case COWGIRL_SITTING_FAST -> "animation.cat.sitting_fast";
            case COWGIRL_SITTING_CUM -> "animation.cat.sitting_cum";
            case HEAD_PAT -> "animation.cat.head_pat";
            default -> "animation.cat.null";
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}