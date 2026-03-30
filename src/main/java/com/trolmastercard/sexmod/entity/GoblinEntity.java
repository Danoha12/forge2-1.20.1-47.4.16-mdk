package com.trolmastercard.sexmod.entity;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

// ... otros imports ...

public class GoblinEntity extends NpcModelCodeEntity implements GeoEntity {

    // ... constantes y campos ...

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        // Evitar interactuar con la Reina si está en el trono
        if (this.isQueen) return InteractionResult.CONSUME;

        // Mecánica de Captura (BJ Mode)
        if (this.getAnimState() == AnimState.RUN) {
            if (this.distanceTo(player) < 3.5F) {
                this.setPartnerUUID(player.getUUID());
                this.setAnimStateFiltered(AnimState.CATCH);
                // Sincronizar con el paquete de red para iniciar la escena
                return InteractionResult.SUCCESS;
            }
        }

        // Mecánica de Carga Estándar
        if (this.getCarrierUUID() == null) {
            this.setCarrierUUID(player.getUUID());
            this.setAnimStateFiltered(AnimState.PICK_UP);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // ── GeckoLib 4: Controladores de Animación Avanzados ─────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Controlador de ojos (Parpadeo)
        registrar.add(new AnimationController<>(this, "eyes", 5, state -> {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.blink"));
        }));

        // Controlador de Movimiento
        registrar.add(new AnimationController<>(this, "movement", 5, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.walk"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.idle"));
        }));

        // Controlador de Acciones y Sex-Anim
        registrar.add(new AnimationController<>(this, "action", 0, this::actionController));
    }

    private PlayState actionController(AnimationState<GoblinEntity> state) {
        AnimState anim = this.getAnimState();
        if (anim == null) return PlayState.STOP;

        // Detectar si el portador está en primera persona para cambiar el modelo de manos
        boolean isFirstPerson = isCarrierInFirstPerson();
        String view = isFirstPerson ? "firstperson" : "thirdperson";

        return switch (anim) {
            case PICK_UP -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.pick_up_" + view));
            case PAIZURI_START -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.paizuri_start"));
            case PAIZURI_FAST -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.goblin.paizuri_fast"));
            case THROWN -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.goblin.thrown"));
            default -> PlayState.STOP;
        };
    }

    private boolean isCarrierInFirstPerson() {
        if (!this.level().isClientSide) return false;
        UUID carrier = this.getCarrierUUID();
        return carrier != null && carrier.equals(Minecraft.getInstance().player.getUUID())
                && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }
}