package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcHandModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.PlayerKoboldEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * BeePlayerKobold - ported from e9.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * PlayerKobold variant for the Bee NPC. Handles:
 *   - sex animation sequence: CITIZEN_START - CITIZEN_SLOW - CITIZEN_FAST - CITIZEN_CUM
 *   - THROW_PEARL / attack / bowcharge / ride animations
 *   - B() / y() = wing-spread open/close hooks
 *
 * Scale: 1.4 wide, 1.3 eye height.
 * Hand texture: textures/entity/bee/hand.png
 * Hand model: BeeHandModel (a7 in original).
 *
 * State-transition guards:
 *   CITIZEN_CUM - cannot go to CITIZEN_FAST or CITIZEN_SLOW (blocks re-entry).
 *
 * Sound keyframes (forwarded to client via SynchedEntityData / sound events):
 *   "attackDone"          - increment attack counter (mod 3)
 *   "pearl"               - if is client and state=THROW_PEARL - send SendCompanionHomePacket
 *   "resetCumPercentage"  - HornyMeterOverlay.reset()
 *   "sex_fastMSG1"        - play pounding sound, HornyMeter += 0.04
 *   "sex_startMSG1"       - play pounding sound, HornyMeter += 0.02
 *   "sex_fastReady"       - if overlay active - N() (show cumming UI)
 *   "sex_fastDone"        - fallthrough to sex_startDone
 *   "sex_startDone"       - transition to CITIZEN_SLOW, ds.d()
 *   "sex_cumMSG1"         - play inflation + pounding sounds
 *   "blackscreen"         - TransitionScreen.show()
 *   "sex_cumDone"         - HornyMeter.reset(), r() (reset state)
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - e9 extends ei - BeePlayerKobold extends PlayerKoboldEntity
 *   - World - Level; UUID stays same
 *   - at - NpcHandModel; a7 - BeeHandModel
 *   - fp - AnimState; y() - getAnimState(); b(fp) - setAnimStateFiltered(fp)
 *   - c(bool) - setWingsOpen(bool)  [B()/y() calls]
 *   - super.b(fp) - super.setAnimStateFiltered(fp)
 *   - AnimationEvent - AnimationState (GeckoLib4)
 *   - AnimationData - AnimatableManager.ControllerRegistrar
 *   - ISoundListener - registerControllers + sound key handling via AnimationController
 *   - ge.b.sendToServer(new gg(f())) - ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getNpcUUID()))
 *   - ds.b/a/d - HornyMeterOverlay.reset/addProgress/show
 *   - d3.d - ClientStateManager.isFreezeActive
 *   - fh.b() - TransitionScreen.show()
 *   - n() - isClientPlayer()
 *   - N() - showCummingOverlay()
 *   - r() - resetSexState()
 *   - c(ModSounds.MISC_POUNDING) - playRandomSound(ModSounds.MISC_POUNDING)
 *   - a(c.a(sound), vol) - playNpcSoundWithVolume(sound, vol)
 *   - this.S - attackCounter; this.C - mainAnimController; this.E - movementController
 */
public class BeePlayerKobold extends PlayerKoboldEntity {

    /** Current attack animation index (0-2). */
    int attackCounter = 1;

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public BeePlayerKobold(Level level) { super(level); }
    public BeePlayerKobold(Level level, UUID uuid) { super(level, uuid); }

    // -- PlayerKoboldEntity contract --------------------------------------------

    @Override
    public float getModelScale() { return 1.4F; }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose p) { return 1.3F; }

    @Override
    public NpcHandModel createHandModel(int slot) { return new BeeHandModel(); }

    @Override
    public String getHandTexturePath(int slot) { return "textures/entity/bee/hand.png"; }

    /** Opens wings (called on mount). */
    public void openWings()  { setWingsOpen(true);  }
    /** Closes wings (called on dismount). */
    public void closeWings() { setWingsOpen(false); }

    // -- Sex-state transition guard ---------------------------------------------

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState current = getAnimState();
        if (current == AnimState.CITIZEN_CUM) {
            if (next == AnimState.CITIZEN_FAST || next == AnimState.CITIZEN_SLOW) return;
        }
        super.setAnimStateFiltered(next);
    }

    // -- Sex trigger (b(String,UUID) in original) -------------------------------

    @Override
    public void onActionSelected(String action, UUID playerId) {
        setSubAnimState(0, AnimState.CITIZEN_START);
        setPartnerSlot(0);
        setAnimStateFiltered(AnimState.CITIZEN_START);
        setPartnerUUID(playerId);

        Player player = level.getPlayerByUUID(playerId);
        if (player == null) return;

        Vec3 offset = getArmHeightOffset(-0.2);
        player.teleportTo(offset.x, offset.y, offset.z);
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        openActionMenu(player, this, new String[]{ "action.names.sex" }, false);
        return true;
    }

    // -- Animation state progression --------------------------------------------

    @Override
    public AnimState getNextState(AnimState current) {
        if (current == AnimState.CITIZEN_SLOW) return AnimState.CITIZEN_FAST;
        return null;
    }

    @Override
    public AnimState getCumState(AnimState current) {
        if (current == AnimState.CITIZEN_FAST) return AnimState.CITIZEN_CUM;
        if (current == AnimState.CITIZEN_SLOW) return AnimState.CITIZEN_CUM;
        return null;
    }

    // -- After cum -------------------------------------------------------------

    @Override
    public void onCumComplete() {
        super.onCumComplete();
        setPartnerSlot(1);
    }

    // -- GeckoLib 4 ------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();

        var movement = new AnimationController<>(this, "movement", 5, this::movementController);
        var action   = new AnimationController<>(this, "action",   0, this::actionController);

        // Sound keyframe listener on the action controller
        action.setSoundKeyframeHandler(event -> handleSoundKeyframe(event.getKeyframeData().getSound()));

        registrar.add(movement, action);
    }

    private PlayState movementController(AnimationState<BeePlayerKobold> state) {
        AnimState anim = getAnimState();
        if (anim != AnimState.NULL) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.null"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.idle"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<BeePlayerKobold> state) {
        AnimState anim = getAnimState();
        if (anim == null) return PlayState.CONTINUE;

        switch (anim) {
            case NULL           -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.null"));
            case CITIZEN_START  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_start"));
            case CITIZEN_SLOW   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_slow"));
            case CITIZEN_FAST   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.sex_fast"));
            case CITIZEN_CUM    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.sex_cum"));
            case THROW_PEARL    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.throw_pearl"));
            case ATTACK         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.attack" + attackCounter));
            case BOW_CHARGE     -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.bowcharge"));
            case RIDE           -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.bee.ride"));
            default             -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.bee.null"));
        }
        return PlayState.CONTINUE;
    }

    // -- Sound keyframe handling ------------------------------------------------

    private void handleSoundKeyframe(String sound) {
        switch (sound) {
            case "attackDone" -> {
                if (++attackCounter == 3) attackCounter = 0;
            }
            case "pearl" -> {
                if (isClientPlayer() && getAnimState() == AnimState.THROW_PEARL) {
                    ModNetwork.CHANNEL.sendToServer(new SendCompanionHomePacket(getNpcUUID()));
                }
            }
            case "resetCumPercentage" -> {
                if (isClientPlayer()) HornyMeterOverlay.reset();
            }
            case "sex_fastMSG1" -> {
                playRandomSound(ModSounds.MISC_POUNDING);
                if (isClientPlayer()) HornyMeterOverlay.addProgress(0.04);
            }
            case "sex_startMSG1" -> {
                playRandomSound(ModSounds.MISC_POUNDING);
                if (isClientPlayer()) HornyMeterOverlay.addProgress(0.02);
            }
            case "sex_fastReady" -> {
                if (isClientPlayer() && ClientStateManager.isFreezeActive()) showCummingOverlay();
            }
            case "sex_fastDone" -> {
                if (!isClientPlayer()) break;
                if (!ClientStateManager.isFreezeActive()) {
                    // Fall through to sex_startDone behaviour
                    setAnimStateFiltered(AnimState.CITIZEN_SLOW);
                    HornyMeterOverlay.show();
                }
            }
            case "sex_startDone" -> {
                setAnimStateFiltered(AnimState.CITIZEN_SLOW);
                if (isClientPlayer()) HornyMeterOverlay.show();
            }
            case "sex_cumMSG1" -> {
                playNpcSoundWithVolume(ModSounds.MISC_CUMINFLATION_SOUND, 2.0F);
                playRandomSound(ModSounds.MISC_POUNDING);
            }
            case "blackscreen" -> {
                if (isClientPlayer()) TransitionScreen.show();
            }
            case "sex_cumDone" -> {
                if (isClientPlayer()) {
                    HornyMeterOverlay.reset();
                    resetSexState();
                }
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return animCache; }
}
