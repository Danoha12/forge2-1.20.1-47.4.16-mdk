package com.trolmastercard.sexmod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * EntityUtil (d) - Static helpers used by NPC targeting/visibility logic.
 *
 *  canTarget(Entity)         - returns false for hostile mobs that NPCs should ignore
 *  hasLineOfSight(Level,Vec3,Entity) - ray-trace from a point to an entity's eyes
 */
public class EntityUtil {

    private EntityUtil() {}

    /**
     * Returns true if the entity is a valid target for NPC interactions.
     * Hostile special-case mobs are excluded.
     */
    public static boolean canTarget(Entity entity) {
        if (entity instanceof Creeper)         return false;
        if (entity instanceof ZombifiedPiglin) return false;
        if (entity instanceof Guardian)        return false;
        if (entity instanceof EnderMan)        return false;
        return true;
    }

    /**
     * Returns true if there is an unobstructed line of sight from {@code from}
     * to the given entity's eye position.
     */
    public static boolean hasLineOfSight(Level level, Vec3 from, Entity entity) {
        Vec3 to = entity.getEyePosition();
        BlockHitResult result = level.clip(new ClipContext(
            from, to,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            entity));
        if (result == null) return true;
        return result.getType() != HitResult.Type.BLOCK;
    }
}
