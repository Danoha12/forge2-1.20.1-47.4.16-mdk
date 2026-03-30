package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.client.model.entity.SlimeHandModel;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CameraControlPacket;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * SlimePlayerKobold — Portado a 1.20.1 / GeckoLib 4.
 * * Entidad Avatar (PlayerKoboldEntity) para la especie Slime.
 * * Maneja las cadenas de Blowjob y Doggy, así como las físicas únicas de este modelo.
 */
public class SlimePlayerKobold extends PlayerKoboldEntity {

    public boolean flyAlt = false;
    public int attackCounter = 1;
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public SlimePlayerKobold(Level level) {
        super(level);
    }

    public SlimePlayerKobold(Level level, UUID uuid) {
        super(level, uuid);
    }

    // ── Propiedades Físicas y Modelos ─────────────────────────────────────────

    @Override public float getModelScale() { return 1.6F; }

    @Override public float getEyeHeight(Pose p) { return 1.64F; }

    // Asumo que tu PlayerKoboldEntity tiene un método base canFly() para sobreescribir
    public boolean canFly() { return false; }

    @Override public boolean autoUnlockSex() { return false; }

    @Override public NpcHandModel createHandModel(int slot) { return new SlimeHandModel(); }

    @Override public String getHandTexturePath(int slot) { return "textures/entity/slime/hand.png"; }

    // ── Interacciones de Jugador (Context Menu) ───────────────────────────────

    @Override
    public boolean onPlayerInteract(Player player) {
        if (this.level().isClientSide) {
            // Asumo que openActionMenu es un método de PlayerKoboldEntity para abrir la GUI
            openActionMenu(player, this, new String[]{ "action.names.blowjob" }, false);
        }
        return true;
    }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        if ("action.names.blowjob".equals(action)) {
            // setSubAnimState probablemente sea un método heredado de NpcInventoryEntity
            setSubAnimState(0, AnimState.SUCKBLOWJOB);
            setAnimStateFiltered(AnimState.SUCKBLOWJOB);
            setPartnerUUID(playerId);
        }
    }

    // ── Lógica de Transición de Cadenas (Speed-Up y Cum) ──────────────────────

    @Override
    public void setAnimStateFiltered(AnimState next) {
        AnimState cur = getAnimState();
        // Bloquear que se regrese a un estado base si ya se corrió
        if (cur == AnimState.CUMBLOWJOB && (next == AnimState.THRUSTBLOWJOB || next == AnimState.SUCKBLOWJOB)) return;
        if (cur == AnimState.DOGGYCUM && (next == AnimState.DOGGYFAST || next == AnimState.DOGGYSLOW)) return;
        super.setAnimStateFiltered(next);
    }

    @Override
    public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.SUCKBLOWJOB) return AnimState.THRUSTBLOWJOB;
        if (cur == AnimState.DOGGYSLOW)   return AnimState.DOGGYFAST;
        return null;
    }

    @Override
    public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.SUCKBLOWJOB || cur == AnimState.THRUSTBLOWJOB) return AnimState.CUMBLOWJOB;
        if (cur == AnimState.DOGGYSLOW || cur == AnimState.DOGGYFAST) return AnimState.DOGGYCUM;
        return null;
    }

    // ── Tick (Manejo de la Cama y Emboscada) ──────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Lógica: Si la Slime está esperando en la cama en pose Doggy
        if (getAnimState() == AnimState.WAITDOGGY) {
            Player target = getTargetPlayer();

            // Si el jugador se acerca a la posición de sexo (offset), la Slime lo captura
            if (target != null && target.position().distanceToSqr(getSexOffsetPos()) <= 1.0D) {
                if (!this.level().isClientSide) {
                    if (target instanceof ServerPlayer sp) {
                        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new CameraControlPacket(false));
                    }

                    setPartnerUUID(target.getUUID());
                    target.setYRot(getStoredYaw());
                    target.setPos(getSexOffsetPos().x, getSexOffsetPos().y, getSexOffsetPos().z);
                    target.setDeltaMovement(Vec3.ZERO);
                    target.setNoGravity(true);

                    setAnimStateFiltered(AnimState.DOGGYSTART);
                }
            }
        }
    }

    // ── GeckoLib 4 Controllers ────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Asumo que ensureControllerInit() es heredado, si no, lo puedes borrar
        // ensureControllerInit();

        // Ojos
        registrar.add(new AnimationController<>(this, "eyes", 5, state -> {
            AnimState current = getAnimState();
            if (current == AnimState.NULL || current == null) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.fhappy"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.null"));
        }));

        // Movimiento
        registrar.add(new AnimationController<>(this, "movement", 5, this::movementController));

        // Acción
        registrar.add(new AnimationController<>(this, "action", 0, this::actionController));
    }

    private PlayState movementController(AnimationState<SlimePlayerKobold> state) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.null"));
        }

        if (a == AnimState.SIT) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.sit"));
        } else if (this.isFallFlying) {
            // Lógica de vuelo alternativa
            String flyAnim = "animation.slime.fly" + (flyAlt ? "2" : "");
            return state.setAndContinue(RawAnimation.begin().thenLoop(flyAnim));
        } else if (state.isMoving()) {
            String moveAnim = this.getDeltaMovement().horizontalDistanceSqr() > 0.04 ? "animation.slime.run" : "animation.slime.walk";
            return state.setAndContinue(RawAnimation.begin().thenLoop(moveAnim));
        } else {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.idle"));
        }
    }

    private PlayState actionController(AnimationState<SlimePlayerKobold> state) {
        AnimState a = getAnimState();
        if (a == null) return PlayState.CONTINUE;

        return switch (a) {
            case UNDRESS       -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.undress"));
            case DRESS         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.dress"));
            case STRIP         -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.strip"));
            case SUCKBLOWJOB   -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.blowjobsuck"));
            case THRUSTBLOWJOB -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.blowjobthrust"));
            case CUMBLOWJOB    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.blowjobcum"));
            case DOGGYGOONBED  -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggygoonbed"));
            case WAITDOGGY     -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggywait"));
            case DOGGYSTART    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggystart"));
            case DOGGYSLOW     -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggyslow"));
            case DOGGYFAST     -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.doggyfast"));
            case DOGGYCUM      -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.doggycum"));
            case ATTACK        -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.attack" + attackCounter));
            case BOW_CHARGE    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.bowcharge"));
            case RIDE          -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.ride"));
            case SIT           -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.slime.sit"));
            default            -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.slime.null"));
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}