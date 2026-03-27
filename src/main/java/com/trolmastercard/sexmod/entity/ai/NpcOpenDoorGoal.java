package com.trolmastercard.sexmod.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.navigation.GroundPathNavigation;

import java.util.EnumSet;

/**
 * NpcOpenDoorGoal - ported from hz.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Allows NPCs to open nearby wooden doors that are along their navigation path.
 * Closes the door again once the entity has passed through.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - EntityAIBase - Goal; setMutexBits - setFlags(EnumSet)
 *   - EntityLiving - Mob
 *   - PathNavigateGround - GroundPathNavigation
 *   - func_75250_a() - canUse()
 *   - func_75253_b() - canContinueToUse()
 *   - func_75249_e() - start()
 *   - func_75246_d() - tick()
 *   - func_75251_c() - stop()
 *   - path.func_75879_b() - path.isDone()
 *   - pathNavigator.func_179686_g() - navigator.isDone() (inverted: !isDone == active)
 *   - path.func_75873_e() - path.getNextNodeIndex()
 *   - path.func_75874_d() - path.getNodeCount()
 *   - path.func_75877_a(i) - path.getNode(i)
 *   - pathPoint.field_75839_a/b/c - node.x/y/z
 *   - entity.func_70092_e(x, y, z) - entity.distanceToSqr(x, y, z)
 *   - DoorBlock.func_176512_a(world, pos, open) - doorBlock.setOpen(entity, world, state, pos, open)
 *   - Material.field_151575_d - Material.WOOD
 *   - BlockPos.field_177992_a - BlockPos.ZERO
 *   - new BlockPos(entity) - entity.blockPosition()
 *   - blockPos.func_177984_a() - blockPos.above()
 */
public class NpcOpenDoorGoal extends Goal {

    protected final Mob mob;
    protected BlockPos doorPos = BlockPos.ZERO;
    protected DoorBlock doorBlock;
    /** True once the door was opened and the entity has crossed through. */
    boolean crossed;
    float initDx;
    float initDz;
    /** Ticks remaining before we close the door again. */
    int closeCountdown = 10;

    public NpcOpenDoorGoal(Mob mob) {
        this.mob = mob;
        if (!(mob.getNavigation() instanceof GroundPathNavigation))
            throw new IllegalArgumentException("Unsupported mob type for NpcOpenDoorGoal");
        setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    // -- Goal lifecycle ---------------------------------------------------------

    @Override
    public boolean canUse() {
        // Scan 10-block radius for wooden doors
        boolean foundDoor = false;
        outer:
        for (int dx = -3; dx < 5; dx++) {
            for (int dz = -3; dz < 5; dz++) {
                BlockPos p = mob.blockPosition().offset(dx, 0, dz);
                BlockState state = mob.level.getBlockState(p);
                if (state.getBlock() instanceof DoorBlock && state.getMaterial() == Material.WOOD) {
                    foundDoor = true;
                    break outer;
                }
            }
        }
        if (!foundDoor) return false;

        GroundPathNavigation nav = (GroundPathNavigation) mob.getNavigation();
        Path path = nav.getPath();
        if (path == null || path.isDone() || !nav.isInProgress()) return false;

        int limit = Math.min(path.getNextNodeIndex() + 2, path.getNodeCount());
        for (int i = 0; i < limit; i++) {
            Node node = path.getNode(i);
            BlockPos candidate = new BlockPos(node.x, node.y + 1, node.z);
            if (mob.distanceToSqr(candidate.getX(), mob.getY(), candidate.getZ()) <= 2.25D) {
                DoorBlock db = getDoorAt(candidate);
                if (db != null) {
                    this.doorPos   = candidate;
                    this.doorBlock = db;
                    return true;
                }
            }
        }

        // Fallback: check position above current block
        BlockPos above = mob.blockPosition().above();
        this.doorPos   = above;
        this.doorBlock = getDoorAt(above);
        return this.doorBlock != null;
    }

    @Override
    public boolean canContinueToUse() {
        return closeCountdown >= 0;
    }

    @Override
    public void start() {
        crossed = false;
        initDx  = (float)(doorPos.getX() + 0.5 - mob.getX());
        initDz  = (float)(doorPos.getZ() + 0.5 - mob.getZ());
        BlockState state = mob.level.getBlockState(doorPos);
        doorBlock.setOpen(mob, mob.level, state, doorPos, true);
    }

    @Override
    public void tick() {
        float dx = (float)(doorPos.getX() + 0.5 - mob.getX());
        float dz = (float)(doorPos.getZ() + 0.5 - mob.getZ());
        // Cross-product sign tells us when the mob has passed the door
        if (initDx * dx + initDz * dz < 0.0F) {
            if (--closeCountdown <= 0) {
                BlockState state = mob.level.getBlockState(doorPos);
                doorBlock.setOpen(mob, mob.level, state, doorPos, false);
                crossed = true;
            }
        }
    }

    @Override
    public void stop() {
        closeCountdown = 10;
    }

    // -- Helpers ----------------------------------------------------------------

    private DoorBlock getDoorAt(BlockPos pos) {
        BlockState state = mob.level.getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof DoorBlock && state.getMaterial() == Material.WOOD) {
            return (DoorBlock) block;
        }
        return null;
    }
}
