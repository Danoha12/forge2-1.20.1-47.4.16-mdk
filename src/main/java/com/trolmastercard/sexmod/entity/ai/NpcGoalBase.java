package com.trolmastercard.sexmod.entity.ai;
import com.trolmastercard.sexmod.ModConstants;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Abstract base Goal shared by all NPC action goals.
 * Ported from f.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Maintains walk/run speeds, a {@link State} enum, and the owner-player
 * reference. Concrete subclasses implement {@link #tick()} by calling
 * {@link #computeState()} and reacting to the returned state.
 */
public abstract class NpcGoalBase extends Goal {

    // -- Speed constants (original g = 0.5, h = 0.7) -------------------------
    public static final double SPEED_WALK = 0.5D;
    public static final double SPEED_RUN  = 0.7D;

    /** Goal cooldown ticks (original b = 60). */
    public static final int COOLDOWN = 60;

    // -- Fields ---------------------------------------------------------------

    /** The NPC this goal belongs to. */
    public BaseNpcEntity entity;

    /** The player that owns / is interacting with the NPC. */
    public Player player;

    /** Cached path navigator. */
    public PathNavigation navigator;

    /** Current goal state. */
    public State state = State.IDLE;

    // -- Constructor ----------------------------------------------------------

    public NpcGoalBase(BaseNpcEntity entity) {
        this.entity    = entity;
        this.navigator = entity.getNavigation();
    }

    // -- Goal lifecycle -------------------------------------------------------

    @Override
    public boolean canUse() {
        String ownerStr = entity.entityData.get(BaseNpcEntity.MASTER_UUID);
        return !ownerStr.isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        String ownerStr = entity.entityData.get(BaseNpcEntity.MASTER_UUID);
        if (ownerStr.isEmpty()) return false;
        try {
            return entity.level().getPlayerByUUID(UUID.fromString(ownerStr)) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void start() {
        this.navigator = entity.getNavigation();
        String ownerStr = entity.entityData.get(BaseNpcEntity.MASTER_UUID);
        try {
            this.player = entity.level().getPlayerByUUID(UUID.fromString(ownerStr));
        } catch (Exception ignored) {}
    }

    @Override
    public void stop() {
        if (navigator != null) navigator.stop();
        this.state = State.IDLE;
        entity.entityData.set(BaseNpcEntity.MASTER_UUID, "");
        this.navigator = null;
        this.player    = null;
    }

    @Override
    public void tick() {
        this.state = computeState();

        // Notify the animation controller
        if (entity.animController != null) {
            entity.animController.setIdle(this.state == State.IDLE);
        }

        applyState(this.state);
    }

    // -- Abstract -------------------------------------------------------------

    /** Compute which state the goal should be in this tick. */
    protected abstract State computeState();

    /** React to the current state (move, attack, etc.). */
    protected abstract void applyState(State state);

    // -- Helpers --------------------------------------------------------------

    /**
     * Teleports the NPC to a random position near the owner if pathfinding
     * fails for too long (original {@code c()}).
     */
    protected void teleportNearPlayer() {
        int tries = 0;
        net.minecraft.core.BlockPos target;
        do {
            target = player.blockPosition().offset(
                    ModConstants.RANDOM.nextInt(10),
                    0,
                    ModConstants.RANDOM.nextInt(10)
            );
        } while (++tries < 20 && !entity.isPathFindable(
                net.minecraft.world.level.pathfinder.BlockPathTypes.WALKABLE));

        if (tries >= 20) {
            entity.teleportTo(player.getX(), player.getY(), player.getZ());
        }
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * Updates walk speed based on player sprint state and distance
     * (original {@code b()}).
     */
    protected double updateSpeed() {
        float dist = entity.distanceTo(player);
        double speed;
        if (player.isSprinting()) {
            speed = SPEED_RUN;
            entity.setAnimationState(BaseNpcEntity.AnimState.RUN);
        } else {
            speed = SPEED_WALK;
            entity.setAnimationState(BaseNpcEntity.AnimState.WALK);
        }
        speed += Math.floor(dist / 5.0F) * 0.2D;

        if (entity.isInWater()) {
            speed *= 60.0D;
            entity.setAnimationState(BaseNpcEntity.AnimState.WALK);
        }

        navigator.setSpeedModifier(speed);
        return speed;
    }

    // -- Death listener - prevent NPC from dying during sex session ------------

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof BaseNpcEntity npc)) return;
        String ownerStr = npc.entityData.get(BaseNpcEntity.MASTER_UUID);
        if (!ownerStr.isEmpty()) {
            event.setCanceled(true);
        }
    }

    // -- State enum -----------------------------------------------------------

    public enum State {
        ATTACK,
        FOLLOW,
        IDLE,
        RIDE,
        DOWNED
    }
}
