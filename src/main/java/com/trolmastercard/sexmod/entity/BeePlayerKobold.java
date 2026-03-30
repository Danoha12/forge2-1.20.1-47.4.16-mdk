package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
// import com.trolmastercard.sexmod.client.gui.InteractionMeterOverlay; // Descomentar cuando la GUI esté lista
// import com.trolmastercard.sexmod.client.gui.TransitionScreen; // Descomentar cuando la GUI esté lista
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.SendCompanionHomePacket;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BeePlayerKobold — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * Variante de PlayerKobold para el NPC Bee. Maneja:
 * - Secuencia de interacción: CITIZEN_START -> CITIZEN_SLOW -> CITIZEN_FAST -> CITIZEN_FINISH.
 * - Animaciones de combate y utilidad.
 * - Keyframes de sonido para sincronización de efectos y GUI.
 */
public class BeePlayerKobold extends PlayerKoboldEntity {

    /** Índice de animación de ataque (0–2). */
    private int attackCounter = 1;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BeePlayerKobold(Level level) { super(level); }
    public BeePlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    // ── PlayerKoboldEntity Contract ────────────────────────────────────────────

    @Override
    public float getModelScale() { return 1.4F; }

    @Override
    public float getEyeHeight(Pose pose) { return 1.3F; }

    // @Override // Descomentar cuando NpcHandModel sea porteado
    // public NpcHandModel createHandModel(int slot) { return new BeeHandModel(); }

    // @Override // Descomentar si existe en la clase base
    // public String getHandTexturePath(int slot) { return "textures/entity/bee/hand.png"; }

    public void openWings()  { setWingsOpen(true);  }
    public void closeWings() { setWingsOpen(false); }

    // ── Interaction Logic ──────────────────────────────────────────────────────

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState current = getAnimState();
        // Evitar que se interrumpa la animación final (FINISH) con estados de loop
        if (current == AnimState.CITIZEN_FINISH) {
            if (next == AnimState.CITIZEN_FAST || next == AnimState.CITIZEN_SLOW) return;
        }
        super.setAnimStateFiltered(next);
    }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        setSubAnimState(0, AnimState.CITIZEN_START);
        // setPartnerSlot(0); // Descomentar si existe en PlayerKoboldEntity
        setAnimStateFiltered(AnimState.CITIZEN_START);
        setPartnerUUID(playerId);

        Player player = level().getPlayerByUUID(playerId);
        if (player == null) return;

        // Teletransporte del jugador a la posición de interacción
        // Asumiendo que getArmHeightOffset existe en la clase base
        Vec3 offset = getArmHeightOffset(-0.2);
        player.teleportTo(offset.x, offset.y, offset.z);
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        // Asumiendo que openActionMenu está en BaseNpcEntity
        openActionMenu(player, this, new String[]{ "action.names.interaction" }, false);
        return true;
    }

    @Override
    public AnimState getNextState(AnimState current) {
        if (current == AnimState.CITIZEN_SLOW) return AnimState.CITIZEN_FAST;
        return null;
    }

    @Override
    public AnimState getCumState(AnimState current) {
        if (current == AnimState.CITIZEN_FAST || current == AnimState.CITIZEN_SLOW) {
            return AnimState.CITIZEN_FINISH;
        }
        return null;
    }

    @Override
    public void onCumComplete() {
        super.onCumComplete();
        // setPartnerSlot(1); // Descomentar si existe
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();

        AnimationController<BeePlayerKobold> movement = new AnimationController<>(this, "movement", 5, this::movementController);
        AnimationController<BeePlayerKobold> action   = new AnimationController<>(this, "action",   0, this::actionController);

        // Registro del manejador de keyframes de sonido para la lógica sincronizada
        action.setSoundKeyframeHandler(event -> handleSoundKeyframe(event.getKeyframeData().getSound()));

        registrar.add(movement, action);
    }

    private PlayState movementController(AnimationState<BeePlayerKobold> state) {
        AnimState anim = getAnimState();
        if (anim != AnimState.NULL && anim != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.null"));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.idle"));
    }

    private PlayState actionController(AnimationState<BeePlayerKobold> state) {
        AnimState anim = getAnimState();
        if (anim == null) return PlayState.CONTINUE;

        switch (anim) {
            case NULL           -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.null"));
            case CITIZEN_START  -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_start"));
            case CITIZEN_SLOW   -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_slow"));
            case CITIZEN_FAST   -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_fast"));
            case CITIZEN_FINISH -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_cum"));
            case THROW_PEARL    -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.throw_pearl"));
            case ATTACK         -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.attack" + attackCounter));
            case BOW            -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.bowcharge"));
            case RIDE           -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.ride"));
            default             -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.null"));
        }
    }

    // ── Sound Keyframe Handling ────────────────────────────────────────────────

    private void handleSoundKeyframe(String soundKey) {
        // Los strings de los casos deben coincidir con los que pusiste en Blockbench
        switch (soundKey) {
            case "attackDone" -> {
                if (++attackCounter >= 3) attackCounter = 0;
            }
            case "pearl" -> {
                if (level().isClientSide() && getAnimState() == AnimState.THROW_PEARL) {
                    ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getNpcUUID()));
                }
            }
            case "resetCumPercentage" -> {
                // if (level().isClientSide()) InteractionMeterOverlay.reset();
            }
            case "sex_fastMSG1" -> {
                playRandomSound(ModSounds.INTERACTION_POUNDING); // Mapeado SFW
                // if (level().isClientSide()) InteractionMeterOverlay.addProgress(0.04);
            }
            case "sex_startMSG1" -> {
                playRandomSound(ModSounds.INTERACTION_POUNDING);
                // if (level().isClientSide()) InteractionMeterOverlay.addProgress(0.02);
            }
            case "sex_fastReady" -> {
                // if (level().isClientSide() && ClientStateManager.isFreezeActive()) showCummingOverlay();
            }
            case "sex_fastDone", "sex_startDone" -> {
                setAnimStateFiltered(AnimState.CITIZEN_SLOW);
                // if (level().isClientSide()) InteractionMeterOverlay.show();
            }
            case "sex_cumMSG1" -> {
                playNpcSoundWithVolume(ModSounds.INTERACTION_FINISH_SOUND, 2.0F);
                playRandomSound(ModSounds.INTERACTION_POUNDING);
            }
            case "blackscreen" -> {
                // if (level().isClientSide()) TransitionScreen.show();
            }
            case "sex_cumDone" -> {
                if (level().isClientSide()) {
                    // InteractionMeterOverlay.reset();
                    resetSexState(); // Asumiendo que está en PlayerKoboldEntity
                }
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}