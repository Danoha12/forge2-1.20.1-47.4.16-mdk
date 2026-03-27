package com.trolmastercard.sexmod.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

/**
 * Ported from df.java (1.12.2 - 1.20.1)
 * EntityAIWatchClosest2 - LookAtPlayerGoal subclass with an enabled toggle.
 *
 * When {@code enabled} is false, the tick() no-ops so the NPC stops tracking.
 */
public class ToggleableWatchClosestGoal extends LookAtPlayerGoal {

    public boolean enabled = true;

    public ToggleableWatchClosestGoal(Mob mob,
                                       Class<? extends LivingEntity> watchClass,
                                       float range,
                                       float probability) {
        super(mob, watchClass, range, probability);
    }

    @Override
    public void tick() {
        if (enabled) {
            super.tick();
        }
    }
}
