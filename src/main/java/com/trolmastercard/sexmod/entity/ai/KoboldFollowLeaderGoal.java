package com.trolmastercard.sexmod.entity.ai;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraft.world.entity.Entity;

/**
 * KoboldFollowLeaderGoal - ported from h.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * NPC goal that makes a kobold follow the tribe leader (the entity tracked
 * by its goal system). Extends NpcGoalBase which provides:
 *   - {@code d} = the NPC entity (BaseNpcEntity)
 *   - {@code a} = the target leader entity
 *   - {@code c} = the path navigator
 *   - {@code f} = current goal state (NpcGoalBase.GoalState)
 *
 * Speed scales up the further the NPC is from its leader (capped at 0.7).
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - EntityAIBase - Goal (handled in NpcGoalBase)
 *   - d.func_70032_d(entity) - d.distanceTo(entity)
 *   - c.func_111269_d() - c.getTargetDistanceSqr() or navigation dist
 *   - c.func_75499_g() - c.stop()
 *   - c.func_75497_a(entity, speed) - c.moveTo(entity, speed)
 *   - d.field_70747_aH = speed - d.getAttribute(MOVEMENT_SPEED).setBaseValue(speed)
 *   - NpcGoalBase.a.FOLLOW / IDLE - NpcGoalBase.GoalState.FOLLOW / IDLE
 */
public class KoboldFollowLeaderGoal extends NpcGoalBase {

    /** Ticks the NPC has been close to the leader while in FOLLOW state. */
    private int closeTimer = 0;
    /** Unused timer, mirrors original 'i' field. */
    private int idleTimer  = 0;

    public KoboldFollowLeaderGoal(BaseNpcEntity npc) {
        super(npc);
    }

    // -- NpcGoalBase contract ----------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
        // Minimum movement speed while following
        this.d.setBaseMovementSpeed(0.02F);
    }

    @Override
    protected NpcGoalBase.GoalState computeDesiredState() {
        float dist = this.d.distanceTo((Entity) this.a);

        boolean tooFar = dist > 5.0F;

        // If the NPC has no sex partner and is close, allow a brief linger period
        // before transitioning back to IDLE (mirrors original j/closeTimer logic).
        if (this.d.getSexPartner() == null) {
            if (!tooFar && this.f == NpcGoalBase.GoalState.FOLLOW) {
                if (++this.closeTimer > 60) {
                    tooFar = false;   // force IDLE transition
                    this.closeTimer = 0;
                } else {
                    tooFar = true;    // keep following for a bit
                }
            }
        }

        return tooFar ? NpcGoalBase.GoalState.FOLLOW : NpcGoalBase.GoalState.IDLE;
    }

    @Override
    protected void onStateChanged(NpcGoalBase.GoalState newState) {
        switch (newState) {
            case FOLLOW -> {
                double dist = this.d.distanceTo((Entity) this.a);
                if (this.c.getDistToTarget() > dist) {
                    this.c.stop();
                    this.c.moveTo((Entity) this.a, 0.5D);
                } else {
                    onStateChanged(NpcGoalBase.GoalState.IDLE);
                }
                this.idleTimer = 300;
                updateSpeed();
            }
            case IDLE -> updateSpeed();
        }
    }

    // -- Speed scaling -----------------------------------------------------------

    /**
     * Scales movement speed with distance: base 0.02, up to 0.7 at long range.
     *
     * Original formula: speed = 0.02 + min(0.7, floor(dist/3) * 0.05)
     */
    private double updateSpeed() {
        float dist  = this.d.distanceTo((Entity) this.a);
        double extra = Math.min(0.7D, Math.floor(dist / 3.0F) * 0.05D);
        float speed  = (float) (0.02F + extra);
        this.d.setBaseMovementSpeed(speed);
        return speed;
    }
}
