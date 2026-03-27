package com.trolmastercard.sexmod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * PhysicsParticle - ported from an.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * A simple per-tick physics simulation for a point-mass particle.
 * Each call to {@link #tick()} applies gravity, drag, and voxel collision.
 *
 * Physical constants (original field names - clean names):
 *   a = 9.81F   - GRAVITY        (m/s- per tick, effectively)
 *   g = 0.05F   - STEP_SIZE      (fraction of velocity per tick)
 *   b = 0.05F   - (same as g, reused)
 *   c = 0.03F   - SURFACE_OFFSET (nudge away from block face on collision)
 *
 * Usage:
 * <pre>
 *   PhysicsParticle p = new PhysicsParticle(level, spawnPos, initialVelocity);
 *   // each tick:
 *   p.tick();
 *   Vec3 current = p.getPosition();
 * </pre>
 */
public class PhysicsParticle {

    public static final float GRAVITY        = 9.81F;
    public static final float STEP_SIZE      = 0.05F;
    public static final float SURFACE_OFFSET = 0.03F;

    private final Level level;

    /** Previous position (last tick). */
    private Vec3 prevPos;

    /** Current position. */
    private Vec3 pos;

    /** Current velocity. */
    private Vec3 velocity;

    public PhysicsParticle(Level level, Vec3 position, Vec3 velocity) {
        this.level    = level;
        this.pos      = position;
        this.prevPos  = position;
        this.velocity = velocity;
    }

    // =========================================================================
    //  Tick
    // =========================================================================

    /**
     * Advances the simulation by one step.
     *
     * Algorithm:
     *  1. If velocity is zero, hold position and return.
     *  2. Apply drag (-0.95) and gravity (-0.4905/tick-) to velocity.
     *  3. Move position by (velocity - STEP_SIZE).
     *  4. Scan the voxel trace between prevPos and pos for the first solid block.
     *  5. On collision, calculate the exact face-intersection point and zero velocity.
     *
     * Original: {@code an.a()}
     */
    public void tick() {
        if (Vec3.ZERO.equals(velocity)) {
            pos = prevPos;
            return;
        }

        // Apply drag + gravity
        velocity = new Vec3(
            velocity.x * 0.95,
            (velocity.y - 0.4905) * 0.95,
            velocity.z * 0.95);

        // Step
        prevPos = pos;
        pos = new Vec3(
            pos.x + velocity.x * STEP_SIZE,
            pos.y + velocity.y * STEP_SIZE,
            pos.z + velocity.z * STEP_SIZE);

        // Collision voxel trace
        BlockPos lastAir   = new BlockPos(prevPos);
        BlockPos firstSolid = null;

        for (BlockPos bp : voxelTrace(new BlockPos(prevPos), new BlockPos(pos))) {
            if (level.getBlockState(bp).getBlock() == Blocks.AIR) {
                lastAir = bp;
            } else {
                firstSolid = bp;
                break;
            }
        }

        if (firstSolid == null) return;   // no collision

        // Determine which face was hit and project the intersection point
        int sx = firstSolid.getX(), lx = lastAir.getX();
        int sy = firstSolid.getY(), ly = lastAir.getY();
        int sz = firstSolid.getZ(), lz = lastAir.getZ();

        if (sx - lx != 0) {
            double face = Math.max(sx, lx);
            double slopeY  = (prevPos.y - pos.y) / (prevPos.x - pos.x);
            double interceptY = pos.y - slopeY * pos.x;
            double hitY = slopeY * face + interceptY;
            double slopeZ  = (prevPos.z - pos.z) / (prevPos.x - pos.x);
            double interceptZ = pos.z - slopeZ * pos.x;
            double hitZ = slopeZ * face + interceptZ;
            pos = new Vec3(face + SURFACE_OFFSET * (sx > lx ? -1 : 1), hitY, hitZ);
            velocity = Vec3.ZERO;
            return;
        }

        if (sy - ly != 0) {
            double face = Math.max(sy, ly);
            double slopeX  = (prevPos.x - pos.x) / (prevPos.y - pos.y);
            double interceptX = pos.x - slopeX * pos.y;
            double hitX = slopeX * face + interceptX;
            double slopeZ  = (prevPos.z - pos.z) / (prevPos.y - pos.y);
            double interceptZ = pos.z - slopeZ * pos.y;
            double hitZ = slopeZ * face + interceptZ;
            pos = new Vec3(hitX, face + SURFACE_OFFSET * (sy > ly ? -1 : 1), hitZ);
            velocity = Vec3.ZERO;
            return;
        }

        if (sz - lz != 0) {
            double face = Math.max(sz, lz);
            double slopeY  = (prevPos.y - pos.y) / (prevPos.z - pos.z);
            double interceptY = pos.y - slopeY * pos.z;
            double hitY = slopeY * face + interceptY;
            double slopeX  = (prevPos.x - pos.x) / (prevPos.z - pos.z);
            double interceptX = pos.x - slopeX * pos.z;
            double hitX = slopeX * face + interceptX;
            pos = new Vec3(hitX, hitY, face + SURFACE_OFFSET * (sz > lz ? -1 : 1));
            velocity = Vec3.ZERO;
        }
    }

    // =========================================================================
    //  Accessors
    // =========================================================================

    public Vec3 getPosition()     { return pos;      }
    public Vec3 getPrevPosition() { return prevPos;   }
    public Vec3 getVelocity()     { return velocity;  }

    // =========================================================================
    //  Voxel line trace (Bresenham 3-D)
    // =========================================================================

    /**
     * Returns all {@link BlockPos} visited on the Bresenham line from
     * {@code from} to {@code to} (inclusive on both ends).
     *
     * Original: {@code an.a(BlockPos, BlockPos)} (static)
     */
    public static List<BlockPos> voxelTrace(BlockPos from, BlockPos to) {
        List<BlockPos> result = new ArrayList<>();
        result.add(from);

        int x = from.getX(), y = from.getY(), z = from.getZ();
        int tx = to.getX(),   ty = to.getY(),   tz = to.getZ();

        int dx = Math.abs(tx - x);
        int dy = Math.abs(ty - y);
        int dz = Math.abs(tz - z);

        int stepX = x < tx ?  1 : -1;
        int stepY = y < ty ?  1 : -1;
        int stepZ = z < tz ?  1 : -1;

        int n    = Math.max(dx, Math.max(dy, dz));
        int errX = n / 2;
        int errY = n / 2;
        int errZ = n / 2;

        for (int i = 0; i < n; i++) {
            result.add(new BlockPos(x, y, z));
            errX -= dx;
            errY -= dy;
            errZ -= dz;
            if (errX < 0) { x += stepX; errX += n; }
            else if (errY < 0) { y += stepY; errY += n; }
            else if (errZ < 0) { z += stepZ; errZ += n; }
        }

        result.add(to);
        return result;
    }
}
