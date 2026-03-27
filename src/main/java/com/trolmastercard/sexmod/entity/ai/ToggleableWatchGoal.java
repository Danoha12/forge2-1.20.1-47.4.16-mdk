package com.trolmastercard.sexmod.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.Entity;

/**
 * ToggleableWatchGoal (df) - Ported from 1.12.2 to 1.20.1.
 *
 * Wraps {@link LookAtPlayerGoal} with a boolean flag {@link #enabled} that
 * lets callers pause the head-tracking behaviour without removing the goal.
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code EntityAIWatchClosest2} - {@code LookAtPlayerGoal}
 *   - {@code EntityLiving} - {@code LivingEntity}
 *   - {@code Entity} class kept as {@code Entity}
 *   - Override {@code tick()} (was {@code func_75246_d}) and guard with flag
 */
public class ToggleableWatchGoal extends LookAtPlayerGoal {

    /** When {@code false}, {@link #tick()} becomes a no-op. */
    public boolean enabled = true;

    public ToggleableWatchGoal(LivingEntity entity,
                               Class<? extends Entity> watchTarget,
                               float maxDistance,
                               float chance) {
        super(entity, watchTarget, maxDistance, chance);
    }

    @Override
    public void tick() {
        if (enabled) {
            super.tick();
        }
    }
}
