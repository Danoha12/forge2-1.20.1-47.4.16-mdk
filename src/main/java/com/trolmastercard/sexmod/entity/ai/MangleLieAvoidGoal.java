package com.trolmastercard.sexmod.entity.ai;

import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * MangleLieAvoidGoal - ported from bt.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A modified {@link AvoidEntityGoal} used by the {@link MangleLieEntity}.
 * The goal is suppressed while:
 *   a) The MangleLie has an active Galath owner ({@code entity.getOwnerEntity() != null}), OR
 *   b) A non-dead, active {@link GalathEntity} is within radius of the MangleLie.
 *
 * Additionally:
 *   - On goal start ({@code start()}): sets the MangleLie's DATA_IS_FLEEING flag to true.
 *   - On goal stop ({@code stop()}) : sets the flag back to false.
 *
 * Field mapping:
 *   a = mangleLie   (MangleLieEntity)
 *   b = radius      (float)
 *
 * In 1.12.2:
 *   - {@code EntityAIAvoidEntity<EntityPlayer>} - {@code AvoidEntityGoal<Player>}
 *   - {@code func_75250_a()} - {@link #canUse()}
 *   - {@code func_75253_b()} - {@link #canContinueToUse()}
 *   - {@code func_75249_e()} - {@link #start()}
 *   - {@code func_75251_c()} - {@link #stop()}
 *   - {@code f8.ar} DataParameter - {@link MangleLieEntity#DATA_IS_FLEEING}
 *   - {@code f8.v()} (owner UUID getter) - {@link MangleLieEntity#getOwnerUUID()}
 *   - {@code f_.k()} (galath isActive) - {@link GalathEntity#isActiveAndReady()}
 *   - {@code blockPos.func_177971_a/func_177973_b(Vec3i)} - {@code AABB(min, max)}
 */
public class MangleLieAvoidGoal extends AvoidEntityGoal<Player> {

    final MangleLieEntity mangleLie;
    final float radius;

    public MangleLieAvoidGoal(MangleLieEntity entity, float radius,
                              double walkSpeed, double sprintSpeed) {
        super(entity, Player.class, radius, walkSpeed, sprintSpeed);
        this.mangleLie = entity;
        this.radius    = radius;
    }

    // =========================================================================
    //  Suppression check
    //  Original: bt.a() - returns true if the goal should be SUPPRESSED
    // =========================================================================

    /**
     * Returns {@code true} if the goal should be blocked from activating:
     *   - MangleLie already has a Galath owner, OR
     *   - An active, non-removed GalathEntity is within the avoidance radius.
     */
    boolean isSuppressed() {
        // If this MangleLie has a bound Galath owner, never flee
        if (mangleLie.getOwnerUUID() != null) return true;

        // Check for any nearby active Galath
        double r = radius;
        AABB searchBox = mangleLie.getBoundingBox().inflate(r);
        List<GalathEntity> galaths = mangleLie.level()
            .getEntitiesOfClass(GalathEntity.class, searchBox);

        for (GalathEntity g : galaths) {
            if (g.level().isClientSide()) continue;
            if (g.isRemoved())            continue;
            if (!g.isActiveAndReady())    continue;
            return true;
        }
        return false;
    }

    // =========================================================================
    //  AvoidEntityGoal overrides
    // =========================================================================

    @Override
    public boolean canUse() {
        if (isSuppressed()) return false;
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        if (isSuppressed()) return false;
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        mangleLie.getEntityData().set(MangleLieEntity.DATA_IS_FLEEING, true);
        super.start();
    }

    @Override
    public void stop() {
        mangleLie.getEntityData().set(MangleLieEntity.DATA_IS_FLEEING, false);
        super.stop();
    }
}
