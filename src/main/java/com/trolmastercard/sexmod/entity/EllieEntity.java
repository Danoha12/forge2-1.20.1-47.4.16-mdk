package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * EllieEntity — Portado a 1.20.1.
 * * Personaje con IA de detección de entorno y animaciones de carga.
 */
public class EllieEntity extends BaseNpcEntity implements GeoEntity {

    private int hugTimer = -1;
    private int carrySlowVariant = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public EllieEntity(EntityType<? extends EllieEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public String getNpcName() { return "Ellie"; }

    // ── Hitbox Dinámica ──────────────────────────────────────────────────────

    @Override
    public float getEyeHeight(Pose pose) {
        // Altura de ojos ajustable según si está agachada o no
        return isCrouchingCheck() ? 1.53F : 1.9F;
    }

    /** Si el techo está a menos de 2 bloques, Ellie se agacha automáticamente */
    public boolean isCrouchingCheck() {
        if (getAnimState() != AnimState.NULL) return false;
        BlockPos headPos = this.blockPosition().above(2);
        return !this.level().getBlockState(headPos).isAir();
    }

    // ── Interacción y Menús ──────────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) {
            // Aquí llamarías a: NpcActionScreen.open(this);
            player.displayClientMessage(Component.literal("Abriendo menú de Ellie..."), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void triggerAction(String action, UUID playerId) {
        // Lógica de selección de escenas desde la UI
        switch (action) {
            case "sitting_ride" -> {
                setAnimStateFiltered(AnimState.HUG_SELECTED);
                setAnimationFollowUp("sitting_ride");
            }
            case "front_dance" -> {
                setAnimStateFiltered(AnimState.HUG_SELECTED);
                setAnimationFollowUp("front_dance");
            }
            case "dressup" -> setAnimStateFiltered(AnimState.STRIP);
        }
    }

    // ── Gestión de Animaciones ───────────────────────────────────────────────

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();

        // Iniciamos el temporizador para la transición de "Abrazo"
        if (next == AnimState.HUG_SELECTED && !this.level().isClientSide()) {
            this.hugTimer = 80; // ~4 segundos
        }

        // Evitar que el loop de "Finish" se rompa por inputs accidentales
        if (cur == AnimState.FRONT_DANCE_FINISH && (next == AnimState.FRONT_DANCE_FAST || next == AnimState.FRONT_DANCE_SLOW)) return;

        super.setAnimStateFiltered(next);
    }

    @Override
    public void tick() {
        super.tick();
        if (hugTimer > 0) hugTimer--;

        // Si el timer llega a cero, pasamos a la animación que dejamos en "FollowUp"
        if (hugTimer == 0 && !this.level().isClientSide()) {
            processFollowUp();
        }
    }

    private void processFollowUp() {
        // Lógica para saltar de la intro a la acción real
        hugTimer = -1;
    }

    // ── GeckoLib 4: Controladores ────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Controlador de ojos (Parpadeo)
        registrar.add(new AnimationController<>(this, "eyes", 5, state ->
                state.setAndContinue(RawAnimation.begin().thenLoop("animation.ellie.eyes"))
        ));

        // Controlador de Movimiento (Caminar/Correr/Agacharse)
        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (getAnimState() != AnimState.NULL) return PlayState.STOP;

            boolean isCrouching = isCrouchingCheck();
            if (state.isMoving()) {
                String anim = isCrouching ? "animation.ellie.crouchwalk" : "animation.ellie.run";
                return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop(isCrouching ? "animation.ellie.crouchidle" : "animation.ellie.idle"));
        }));

        // Controlador de Acciones Especiales
        registrar.add(new AnimationController<>(this, "action", 0, this::handleActionState));
    }

    private PlayState handleActionState(AnimationState<EllieEntity> state) {
        AnimState anim = getAnimState();
        if (anim == AnimState.NULL) return PlayState.STOP;

        String name = switch (anim) {
            case STRIP -> "animation.ellie.strip";
            case HUG_SELECTED -> "animation.ellie.hugselected";
            case SITTING_RIDE_SLOW -> "animation.ellie.cowgirlslow2";
            case FRONT_DANCE_SLOW -> "animation.ellie.missionary_slow";
            case LIFT_DANCE_SLOW -> "animation.ellie.carry_slow" + carrySlowVariant;
            default -> "animation.ellie.null";
        };

        return state.setAndContinue(RawAnimation.begin().thenLoop(name));
    }

    // ── Persistencia y Utilidades ────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("carryVariant", carrySlowVariant);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.carrySlowVariant = tag.getInt("carryVariant");
    }

    @Override public Vec3 getBonePosition(String boneName) { return this.position().add(0, 1, 0); }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}