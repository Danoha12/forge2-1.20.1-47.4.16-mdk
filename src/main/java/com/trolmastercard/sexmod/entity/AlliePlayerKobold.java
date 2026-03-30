package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * AlliePlayerKobold — Portado a 1.20.1 y enmascarado (SFW).
 *
 * Entidad específica para "Allie".
 * Cadenas de animación enmascaradas:
 * Deep Gift:    DEEP_GIFT_START -> DEEP_GIFT_SLOW -> DEEP_GIFT_FAST -> DEEP_GIFT_FINISH
 * Reverse Ride: REVERSE_RIDE_START -> REVERSE_RIDE_SLOW -> REVERSE_RIDE_FAST_START -> REVERSE_RIDE_FINISH
 *
 * Emite partículas PORTAL durante DEEP_GIFT_FAST.
 */
public class AlliePlayerKobold extends PlayerKoboldEntity {

    public float scaleAdjust = 0.0F;
    public Player targetPlayer = null;
    public boolean invoked = false;
    public int attackCounter = 1;
    public int ar = 1; // variant suffix for slow
    public int av = 1; // variant suffix for fast

    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public AlliePlayerKobold(Level level) {
        super(level);
    }

    public AlliePlayerKobold(Level level, UUID uuid) {
        super(level, uuid);
    }

    @Override
    public float getModelScale() {
        return 1.9F + scaleAdjust;
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return 1.63F;
    }

    @Override
    public boolean canFly() {
        return false;
    }

    // @Override // Descomentar cuando NpcHandModel sea porteado
    // public NpcHandModel createHandModel(int slot) { return new AllieHandModel(); }

    // @Override // Descomentar si el método existe en PlayerKoboldEntity
    // public String getHandTexturePath(int slot) { return "textures/entity/allie/hand.png"; }

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "action.names.deepthroat" -> {
                setAnimStateFiltered(AnimState.DEEP_GIFT_START);
                setSubAnimState(getArmHeightSlot(), AnimState.DEEP_GIFT_START);
                setPartnerUUID(playerId);
            }
            case "Reverse cowgirl" -> {
                setAnimStateFiltered(AnimState.REVERSE_RIDE_START);
                setSubAnimState(0, AnimState.REVERSE_RIDE_START);
                setPartnerUUID(playerId);
            }
        }
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        // Asumiendo que openActionMenu está en BaseNpcEntity
        openActionMenu(player, this, new String[]{ "action.names.deepthroat" }, false);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // Portal particles during DEEP_GIFT_FAST
        if (this.level().isClientSide() && getAnimState() == AnimState.DEEP_GIFT_FAST) {
            double radius = 4.0;
            for (int i = 0; i < 2; i++) {
                double angle = this.random.nextDouble() * Math.PI * 2;
                this.level().addParticle(ParticleTypes.PORTAL,
                        getX() + Math.cos(angle) * radius * this.random.nextDouble(),
                        getY() + this.random.nextDouble() * radius,
                        getZ() + Math.sin(angle) * radius * this.random.nextDouble(),
                        0, 0, 0);
            }
        }
    }

    // ── Lógica de progresión (Usando AnimState SFW) ──────────────────────────

    // @Override // Descomentar si getNextState está en PlayerKoboldEntity
    public AnimState getNextState(AnimState cur) {
        if (cur == AnimState.DEEP_GIFT_SLOW)       return AnimState.DEEP_GIFT_FAST;
        if (cur == AnimState.REVERSE_RIDE_SLOW)    return AnimState.REVERSE_RIDE_FAST_START; // Sustituye FASTC
        // if (cur == AnimState.REVERSE_RIDE_FAST_START) return AnimState.REVERSE_RIDE_FAST_CONTINUES; // Sustituye FASTS
        return null;
    }

    // @Override // Descomentar si getCumState está en PlayerKoboldEntity
    public AnimState getCumState(AnimState cur) {
        if (cur == AnimState.DEEP_GIFT_SLOW || cur == AnimState.DEEP_GIFT_FAST)
            return AnimState.DEEP_GIFT_FINISH;

        if (cur == AnimState.REVERSE_RIDE_SLOW || cur == AnimState.REVERSE_RIDE_FAST_START)
            return AnimState.REVERSE_RIDE_FINISH;

        return null;
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        ensureControllerInit();
        registrar.add(
                new AnimationController<>(this, "eyes", 5, state -> {
                    AnimState as = getAnimState();
                    if (as == AnimState.NULL || as == null) {
                        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.bia.blink"));
                    }
                    return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
                }),
                new AnimationController<>(this, "tail", 5, state ->
                        state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.tail"))
                ),
                new AnimationController<>(this, "movement", 5, this::movementController),
                new AnimationController<>(this, "action", 0, this::actionController)
        );
    }

    private PlayState movementController(AnimationState<AlliePlayerKobold> state) {
        AnimState a = getAnimState();
        if (a != AnimState.NULL && a != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
    }

    private PlayState actionController(AnimationState<AlliePlayerKobold> state) {
        AnimState a = getAnimState();
        if (a == null) return PlayState.CONTINUE;

        // Mapeo del Enum SFW a los nombres originales de los JSONs de Blockbench
        switch (a) {
            case SUMMON                 -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon"));
            case SUMMON_NORMAL          -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon_normal"));
            case SUMMON_NORMAL_WAIT     -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.summon_normal_wait"));
            case SUMMON_WAIT            -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.summon_wait"));

            // Enmascarados (DEEP_GIFT) -> Llamando a los JSON explícitos originales
            // Nota: En AnimState no pusimos PREPARE, si los necesitas agrégalos, aquí mapeo los existentes.
            case DEEP_GIFT_START        -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_start"));
            case DEEP_GIFT_SLOW         -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.deepthroat_slow"));
            case DEEP_GIFT_FAST         -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.deepthroat_fast"));
            case DEEP_GIFT_FINISH       -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_cum"));

            case RICH_FIRST_TIME        -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.rich")); // Asumiendo RICH = RICH_FIRST_TIME
            case RICH_NORMAL            -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.rich_normal"));
            case SUMMON_SAND            -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon_sand"));
            case ATTACK                 -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.attack" + attackCounter));
            case BOW                    -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.bowcharge")); // BOW_CHARGE = BOW

            // Enmascarados (REVERSE_RIDE)
            case REVERSE_RIDE_START     -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_start"));
            case REVERSE_RIDE_SLOW      -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_slow" + ar));
            case REVERSE_RIDE_FAST_START-> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_fastc" + av)); // FASTC -> FAST_START
            case REVERSE_RIDE_FINISH    -> return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_cum"));

            default                     -> return state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.null"));
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}