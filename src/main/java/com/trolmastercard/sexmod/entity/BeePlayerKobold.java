package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.SendCompanionHomePacket;
import com.trolmastercard.sexmod.registry.ModSounds;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BeePlayerKobold — Portado a 1.20.1.
 * Código corregido: Switch expressions modernos y limpieza de sintaxis.
 */
public class BeePlayerKobold extends PlayerKoboldEntity {

    private int attackCounter = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BeePlayerKobold(Level level) { super(level); }
    public BeePlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    @Override
    public float getModelScale() { return 1.4F; }

    @Override
    public float getEyeHeight(Pose pose) { return 1.3F; }

    public void openWings()  { setWingsOpen(true);  }
    public void closeWings() { setWingsOpen(false); }

    // ── Lógica de Interacción ──────────────────────────────────────────────────

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState current = getAnimState();
        if (current == AnimState.CITIZEN_CUM) {
            if (next == AnimState.CITIZEN_FAST || next == AnimState.CITIZEN_SLOW) return;
        }
        super.setAnimStateFiltered(next);
    }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        setAnimStateFiltered(AnimState.CITIZEN_START);
        setPartnerUUID(playerId);

        Player player = level().getPlayerByUUID(playerId);
        if (player != null) {
            Vec3 offset = getPosition().add(0, 0.5, 0); // Ajuste temporal de posición
            player.teleportTo(offset.x, offset.y, offset.z);
        }
    }

    @Override
    public boolean onPlayerInteract(Player player) {
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
            return AnimState.CITIZEN_CUM;
        }
        return null;
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        AnimationController<BeePlayerKobold> movement = new AnimationController<>(this, "movement", 5, this::movementController);
        AnimationController<BeePlayerKobold> action   = new AnimationController<>(this, "action",   0, this::actionController);

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

        // 🚀 Switch Expression corregido para Java 17
        return switch (anim) {
            case NULL           -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.null"));
            case CITIZEN_START  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_start"));
            case CITIZEN_SLOW   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_slow"));
            case CITIZEN_FAST   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_fast"));
            case CITIZEN_CUM    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_cum"));
            case THROW_PEARL    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.throw_pearl"));
            case ATTACK         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.attack" + attackCounter));
            case BOW            -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.bowcharge"));
            case RIDE           -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.ride"));
            default             -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.null"));
        };
    }

    // ── Manejo de Sonidos corregido ────────────────────────────────────────────

    private void handleSoundKeyframe(String soundKey) {
        // En métodos void, el switch con flechas '->' es la mejor opción
        switch (soundKey) {
            case "attackDone" -> {
                if (++attackCounter >= 3) attackCounter = 0;
            }
            case "pearl" -> {
                if (level().isClientSide() && getAnimState() == AnimState.THROW_PEARL) {
                    ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getNpcUUID()));
                }
            }
            case "sex_fastMSG1", "sex_startMSG1" -> playRandomSound(ModSounds.INTERACTION_POUNDING);
            case "sex_fastDone", "sex_startDone" -> setAnimStateFiltered(AnimState.CITIZEN_SLOW);
            case "sex_cumMSG1" -> {
                playNpcSoundWithVolume(ModSounds.INTERACTION_FINISH_SOUND, 2.0F);
                playRandomSound(ModSounds.INTERACTION_POUNDING);
            }
            case "sex_cumDone" -> {
                if (level().isClientSide()) resetSexState();
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}