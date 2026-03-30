package com.trolmastercard.sexmod.client.renderer;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * PhysicsParticle — Portado a 1.20.1.
 * * Simulación de física de punto-masa con colisión por vóxeles.
 */
public class PhysicsParticle {

    public static final float STEP_SIZE = 0.05F;
    public static final float SURFACE_OFFSET = 0.03F;

    private final Level level;
    private Vec3 prevPos;
    private Vec3 pos;
    private Vec3 velocity;

    private int age;
    private final int maxLifetime;
    private boolean dead = false;

    public PhysicsParticle(Level level, Vec3 position, Vec3 velocity, int lifetime) {
        this.level = level;
        this.pos = position;
        this.prevPos = position;
        this.velocity = velocity;
        this.maxLifetime = lifetime;
        this.age = 0;
    }

    // =========================================================================
    //  Lógica de Simulación (Tick)
    // =========================================================================

    public void tick() {
        this.age++;
        if (this.age >= this.maxLifetime) {
            this.dead = true;
            return;
        }

        if (velocity.lengthSqr() < 0.0001D) {
            velocity = Vec3.ZERO;
            prevPos = pos;
            return;
        }

        // 1. Aplicar Gravedad y Fricción (Drag)
        // El valor 0.4905 es la gravedad ajustada por tick
        velocity = new Vec3(
                velocity.x * 0.95,
                (velocity.y - 0.4905) * 0.95,
                velocity.z * 0.95
        );

        // 2. Calcular siguiente posición
        prevPos = pos;
        Vec3 nextPos = pos.add(velocity.scale(STEP_SIZE));

        // 3. Trazado de colisión (Voxel Trace)
        BlockPos startBP = BlockPos.containing(pos);
        BlockPos endBP = BlockPos.containing(nextPos);

        if (!startBP.equals(endBP)) {
            List<BlockPos> path = voxelTrace(startBP, endBP);
            for (BlockPos bp : path) {
                BlockState state = level.getBlockState(bp);
                // Si chocamos con algo que no sea aire o fluido
                if (!state.isAir() && state.getFluidState().isEmpty()) {
                    handleCollision(bp);
                    return;
                }
            }
        }

        this.pos = nextPos;
    }

    private void handleCollision(BlockPos hitBlock) {
        // Al chocar, la partícula se detiene y se queda pegada a la superficie
        this.velocity = Vec3.ZERO;

        // Ajustamos la posición ligeramente para que no quede "dentro" del bloque
        // y evitar el z-fighting (parpadeo de textura)
        Direction side = getCollisionDirection(BlockPos.containing(prevPos), hitBlock);
        this.pos = new Vec3(
                hitBlock.getX() + 0.5 + (side.getStepX() * (0.5 + SURFACE_OFFSET)),
                hitBlock.getY() + 0.5 + (side.getStepY() * (0.5 + SURFACE_OFFSET)),
                hitBlock.getZ() + 0.5 + (side.getStepZ() * (0.5 + SURFACE_OFFSET))
        );
    }

    // =========================================================================
    //  Utilidades Matemáticas
    // =========================================================================

    private net.minecraft.core.Direction getCollisionDirection(BlockPos lastAir, BlockPos hitBlock) {
        return net.minecraft.core.Direction.getNearest(
                hitBlock.getX() - lastAir.getX(),
                hitBlock.getY() - lastAir.getY(),
                hitBlock.getZ() - lastAir.getZ()
        ).getOpposite();
    }

    public boolean isDead() { return dead; }
    public Vec3 getPos() { return pos; }
    public Vec3 getPrevPos() { return prevPos; }

    /** Algoritmo Bresenham 3D para encontrar bloques en la trayectoria */
    public static List<BlockPos> voxelTrace(BlockPos from, BlockPos to) {
        List<BlockPos> path = new ArrayList<>();
        int x = from.getX(), y = from.getY(), z = from.getZ();
        int dx = Math.abs(to.getX() - x), dy = Math.abs(to.getY() - y), dz = Math.abs(to.getZ() - z);
        int sx = x < to.getX() ? 1 : -1, sy = y < to.getY() ? 1 : -1, sz = z < to.getZ() ? 1 : -1;
        int err1 = dx - dy, err2 = dx - dz;

        for (int i = 0; i < (dx + dy + dz); i++) {
            path.add(new BlockPos(x, y, z));
            if (x == to.getX() && y == to.getY() && z == to.getZ()) break;

            int e1 = err1, e2 = err2;
            if (e1 > -dy) { err1 -= dy; err2 -= dz; x += sx; }
            else if (e2 > -dz) { err1 += dx; z += sz; } // El orden importa para no saltar bloques
            else { err2 += dx; y += sy; }
        }
        return path;
    }
}