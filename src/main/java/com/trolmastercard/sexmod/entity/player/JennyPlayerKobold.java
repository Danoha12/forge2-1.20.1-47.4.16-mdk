package com.trolmastercard.sexmod.entity.player;

import com.trolmastercard.sexmod.client.model.JennyHandModel;
import com.trolmastercard.sexmod.client.model.JennyHandNudeModel;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.network.packet.NpcActionQueuePacket;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * JennyPlayerKobold — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * Define el comportamiento de un Kobold transformado en Jenny.
 * - Escala: 1.75x.
 * - Cadenas de interacción: Special A (Blow), Special B (Paizuri), Special C (Doggy).
 * - Lógica de detección de proximidad para inicio de secuencias.
 */
public class JennyPlayerKobold extends PlayerKoboldEntity implements GeoEntity {

    private boolean flyAlt = false;
    private boolean interactionCHard = false;
    private boolean interactionBCam = false;
    private int attackVariant = 1;
    private boolean actionSent = false;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public JennyPlayerKobold(Level level) { super(level); }
    public JennyPlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override public float getModelScale() { return 1.75F; }
    @Override public float getEyeHeight(Pose pose) { return 1.64F; }

    @Override
    public NpcHandModel createHandModel(int slot) {
        return slot == 0 ? new JennyHandNudeModel() : new JennyHandModel();
    }

    @Override
    public String getHandTexturePath(int slot) {
        return slot == 0 ? "textures/entity/jenny/hand_nude.png" : "textures/entity/jenny/hand.png";
    }

    // ── Lógica de Interacciones ───────────────────────────────────────────────

    @Override
    public boolean onPlayerInteract(Player player) {
        // Mapeo SFW: Opciones de interacción especial
        openActionMenu(player, this, new String[]{ "action.names.special_A", "action.names.special_B" }, false);
        return true;
    }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "action.names.special_B" -> {
                this.getEntityData().set(BaseNpcEntity.MODEL_INDEX, 0);
                setAnimStateFiltered(AnimState.INTERACTION_B_START);
                setPartnerUUID(playerId);
            }
            case "action.names.special_A" -> {
                setAnimStateFiltered(AnimState.INTERACTION_A_START);
                setPartnerUUID(playerId);
            }
        }

        // Sincronización con el servidor
        if (getOwnerUUID() != null) {
            ModNetwork.CHANNEL.sendToServer(new NpcActionQueuePacket(action, playerId, getOwnerUUID(), actionSent));
            actionSent = true;
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Lógica de "Enganche" para la interacción en cama (C)
        if (getAnimState() == AnimState.INTERACTION_WAIT_C) {
            Player target = getTargetPlayer();
            if (target != null && target.distanceToSqr(getSexOffsetPos()) < 1.0) {
                if (!this.level().isClientSide()) {
                    handleInteractionCStart(target);
                }
            }
        }
    }

    private void handleInteractionCStart(Player target) {
        // SFW: Mensaje de restricción de tipo de jugador
        if (isPartnerTypeInvalid(target)) {
            target.sendSystemMessage(Component.translatable("mod.message.interaction_unavailable").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }

        setPartnerUUID(target.getUUID());
        target.setPos(getX(), getSexOffsetPos().y, getZ());
        target.setDeltaMovement(Vec3.ZERO);

        // Bloquear movimiento y preparar escena
        setAnimStateFiltered(AnimState.INTERACTION_START_C);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) target), new CameraControlPacket(true));
    }

    private boolean isPartnerTypeInvalid(Player p) { return false; } // Placeholder para lógica de género/tipo

    // ── Progresión de Estados ─────────────────────────────────────────────────

    @Override
    public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.INTERACTION_LOOP_A) return AnimState.INTERACTION_THRUST_A;
        if (cur == AnimState.INTERACTION_SLOW_C) return AnimState.INTERACTION_FAST_C;
        if (cur == AnimState.INTERACTION_SLOW_B) return AnimState.INTERACTION_FAST_B;
        return null;
    }

    @Override
    public AnimState getFinishState(AnimState cur) {
        if (cur == AnimState.INTERACTION_LOOP_A || cur == AnimState.INTERACTION_THRUST_A) return AnimState.INTERACTION_FINISH_A;
        if (cur == AnimState.INTERACTION_SLOW_C || cur == AnimState.INTERACTION_FAST_C) return AnimState.INTERACTION_FINISH_C;
        if (cur == AnimState.INTERACTION_SLOW_B || cur == AnimState.INTERACTION_FAST_B) return AnimState.INTERACTION_FINISH_B;
        return null;
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "eyes", 5, state -> {
                    AnimState as = getAnimState();
                    return state.setAndContinue(as == AnimState.NULL || as == null
                            ? RawAnimation.begin().thenLoop("animation.jenny.fhappy")
                            : RawAnimation.begin().thenLoop("animation.jenny.null"));
                }),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action", 0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<JennyPlayerKobold> state) {
        AnimState as = getAnimState();
        if (as != AnimState.NULL && as != null) return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.null"));

        if (this.isFallFlying()) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.fly" + (flyAlt ? "2" : "")));
        }

        if (state.isMoving()) {
            String walkAnim = getDeltaMovement().horizontalDistanceSqr() > 0.04 ? "animation.jenny.run" : "animation.jenny.fastwalk";
            return state.setAndContinue(RawAnimation.begin().thenLoop(walkAnim));
        }

        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.jenny.idle"));
    }

    private PlayState actionController(AnimationState<JennyPlayerKobold> state) {
        AnimState as = getAnimState();
        if (as == null) return PlayState.CONTINUE;

        String anim = switch (as) {
            case INTERACTION_START_A -> "animation.jenny.blowjobintro";
            case INTERACTION_LOOP_A -> "animation.jenny.blowjobsuck";
            case INTERACTION_THRUST_A -> "animation.jenny.blowjobthrust";
            case INTERACTION_FINISH_A -> "animation.jenny.blowjobcum";

            case INTERACTION_WAIT_C -> "animation.jenny.doggywait";
            case INTERACTION_START_C -> "animation.jenny.doggystart";
            case INTERACTION_SLOW_C -> "animation.jenny.doggyslow";
            case INTERACTION_FAST_C -> "animation.jenny.doggyfast_" + (interactionCHard ? "hard" : "soft");
            case INTERACTION_FINISH_C -> "animation.jenny.doggycum";

            case INTERACTION_B_START -> "animation.jenny.paizuri_start";
            case INTERACTION_SLOW_B -> "animation.jenny.paizuri_slow";
            case INTERACTION_FINISH_B -> "animation.jenny.paizuri_cum";

            case ATTACK -> "animation.jenny.attack" + attackVariant;
            default -> "animation.jenny.null";
        };

        return state.setAndContinue(RawAnimation.begin().thenLoop(anim));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}