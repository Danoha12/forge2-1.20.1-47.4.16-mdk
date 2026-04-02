package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

import static com.trolmastercard.sexmod.registry.AnimState.*;

/**
 * AlliePlayerKobold — Portado a 1.20.1.
 * Código corregido para evitar errores de "orphaned case" y llaves mal cerradas.
 */
public class AlliePlayerKobold extends PlayerKoboldEntity {

    public float scaleAdjust = 0.0F;
    public Player targetPlayer = null;
    public boolean invoked = false;
    public int attackCounter = 1;
    public int ar = 1; // variant slow
    public int av = 1; // variant fast

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

    @Override
    public void onActionSelected(String action, UUID playerId) {
        switch (action) {
            case "action.names.deepthroat" -> {
                setAnimStateFiltered(DEEPTHROAT_START);
                setPartnerUUID(playerId);
            }
            case "Reverse cowgirl" -> {
                setAnimStateFiltered(REVERSE_COWGIRL_START);
                setPartnerUUID(playerId);
            }
        }
    }

    @Override
    public boolean onPlayerInteract(Player player) {
        openActionMenu(player, this, new String[]{ "action.names.deepthroat", "Reverse cowgirl" }, false);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        // Partículas durante DEEPTHROAT_FAST
        if (this.level().isClientSide() && getAnimState() == DEEPTHROAT_FAST) {
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

    // ── Lógica de progresión ───────────────────────────────────────────────

    public AnimState getNextState(AnimState cur) {
        if (cur == DEEPTHROAT_SLOW) return DEEPTHROAT_FAST;
        if (cur == REVERSE_COWGIRL_SLOW) return REVERSE_COWGIRL_FAST_CONTINUES;
        return null;
    }

    public AnimState getCumState(AnimState cur) {
        if (cur == DEEPTHROAT_SLOW || cur == DEEPTHROAT_FAST) return DEEPTHROAT_CUM;
        if (cur == REVERSE_COWGIRL_SLOW || cur == REVERSE_COWGIRL_START) return REVERSE_COWGIRL_CUM;
        return null;
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(
                new AnimationController<>(this, "eyes", 5, state -> {
                    AnimState as = getAnimState();
                    if (as == NULL || as == null) {
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
        AnimState current = getAnimState();
        if (current != NULL && current != null) {
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.null"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<AlliePlayerKobold> state) {
        AnimState current = getAnimState();
        if (current == null) return PlayState.CONTINUE;

        return switch (current) {
            case SUMMON             -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon"));
            case SUMMON_NORMAL      -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon_normal"));
            case SUMMON_NORMAL_WAIT -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.summon_normal_wait"));
            case SUMMON_WAIT        -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.summon_wait"));

            case DEEPTHROAT_START   -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_start"));
            case DEEPTHROAT_SLOW    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.deepthroat_slow"));
            case DEEPTHROAT_FAST    -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.deepthroat_fast"));
            case DEEPTHROAT_CUM     -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.deepthroat_cum"));

            case RICH_FIRST_TIME    -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.rich"));
            case RICH_NORMAL        -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.rich_normal"));
            case SUMMON_SAND        -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.summon_sand"));
            case ATTACK             -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.attack" + this.attackCounter));
            case BOW                -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.bowcharge"));

            case REVERSE_COWGIRL_START          -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_start"));
            case REVERSE_COWGIRL_SLOW           -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_slow" + this.ar));
            case REVERSE_COWGIRL_FAST_CONTINUES -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_fastc" + this.av));
            case REVERSE_COWGIRL_CUM            -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.allie.reverse_cowgirl_cum"));

            default -> state.setAndContinue(RawAnimation.begin().thenPlay("animation.allie.null"));
        };
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animCache;
    }
}