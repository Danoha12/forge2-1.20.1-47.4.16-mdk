package com.trolmastercard.sexmod.entity.ai;
import com.trolmastercard.sexmod.KoboldEntity;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * TribeAttackGoal - ported from aa.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * {@link NearestAttackableTargetGoal} specialization that targets nearby
 * {@link KoboldEntity} instances but only selects ones that are NOT currently
 * in an active sex animation (i.e. {@code ff.J()} - {@code kobold.isAvailableToAttack()}).
 *
 * Additional behaviour:
 *  - {@code mustSee} version checks entity health: if attacker is below 50% HP
 *    and {@code healthCheck} is true, the goal will not start.
 *  - Random tick suppression: when {@code tickRate > 0}, the goal will only
 *    evaluate on a {@code 1-in-tickRate} chance each tick (same as vanilla).
 *
 * Constructor mapping:
 *   aa(EntityCreature, bool mustSee, bool healthCheck)
 *     - TribeAttackGoal(PathfinderMob, boolean mustSee, boolean healthCheck)
 *   aa(EntityCreature, bool mustSee, bool shouldSortByDistance, bool healthCheck)
 *     - TribeAttackGoal(PathfinderMob, boolean mustSee, boolean sortByDist, boolean healthCheck)
 *   aa(EntityCreature, int tickRate, bool mustSee, bool sortByDist, Predicate, bool healthCheck)
 *     - TribeAttackGoal(PathfinderMob, int tickRate, boolean mustSee, boolean sortByDist,
 *                        Predicate<LivingEntity>, boolean healthCheck)
 */
public class TribeAttackGoal extends NearestAttackableTargetGoal<KoboldEntity> {

    private final int     tickRate;
    private final boolean healthCheck;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public TribeAttackGoal(PathfinderMob attacker,
                            boolean mustSee,
                            boolean healthCheck) {
        this(attacker, mustSee, false, healthCheck);
    }

    public TribeAttackGoal(PathfinderMob attacker,
                            boolean mustSee,
                            boolean sortByDistance,
                            boolean healthCheck) {
        this(attacker, 10, mustSee, sortByDistance, null, healthCheck);
    }

    public TribeAttackGoal(PathfinderMob attacker,
                            int tickRate,
                            boolean mustSee,
                            boolean sortByDistance,
                            @Nullable Predicate<LivingEntity> targetPredicate,
                            boolean healthCheck) {
        super(attacker, KoboldEntity.class, tickRate, mustSee, sortByDistance, targetPredicate);
        this.tickRate    = tickRate;
        this.healthCheck = healthCheck;
    }

    // =========================================================================
    //  Goal logic
    // =========================================================================

    @Override
    public boolean canUse() {
        // Health-gate: don't attack if attacker is below 50% HP
        if (healthCheck && mob.getHealth() < mob.getMaxHealth() * 0.5F) {
            return false;
        }

        // Random tick suppression (mirrors vanilla EntityAINearestAttackableTarget)
        if (tickRate > 0 && mob.getRandom().nextInt(tickRate) != 0) {
            return false;
        }

        // Collect nearby kobolds
        List<KoboldEntity> candidates = mob.level().getEntitiesOfClass(
            KoboldEntity.class,
            getTargetSearchArea(getFollowDistance()),
            targetConditions);

        if (candidates.isEmpty()) return false;

        // Filter: only target kobolds that are available (not mid-animation)
        List<KoboldEntity> available = candidates.stream()
            .filter(KoboldEntity::isAvailableToAttack)
            .sorted(Comparator.comparingDouble(k -> k.distanceTo(mob)))
            .toList();

        if (available.isEmpty()) return false;

        target = available.get(0);
        return true;
    }
}
