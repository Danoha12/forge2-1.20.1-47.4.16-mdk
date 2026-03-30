package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.client.FakeWorld;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * NpcWorldUtil — Portado a 1.20.1.
 * * Utilidades geográficas y de entorno para la IA y el renderizado de NPCs.
 */
public class NpcWorldUtil {

    // ── Cálculos de Ángulo ───────────────────────────────────────────────────

    public static float angleDelta(float from, float to) {
        from = MathUtil.normalizeAngle(from);
        to = MathUtil.normalizeAngle(to);
        float diff = Math.abs(from - to);
        float diffAlt = (float) (Math.PI * 2) - diff;
        float smallest = Math.min(diff, diffAlt);
        return (from > to) ? -smallest : smallest;
    }

    // ── Iluminación Dinámica (Sexmod Mode) ───────────────────────────────────

    /** * Calcula la dirección de la luz más brillante alrededor del NPC.
     * Se usa para que el brillo en la piel de las chicas siga las antorchas o el sol.
     */
    public static Vec3 computeSexmodLightDir(LivingEntity entity, float partial) {
        Level world = entity.level();
        if (world instanceof FakeWorld) return new Vec3(0, 1, 0);

        BlockPos center = entity.blockPosition();
        Map<Vec3, Integer> brightMap = new HashMap<>();
        int maxLight = 0;

        // Escaneo de 3x3x3 para encontrar el punto de luz más fuerte
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int light = world.getMaxLocalRawBrightness(center.offset(dx, dy, dz));
                    brightMap.put(new Vec3(dx, dy, dz), light);
                    if (light > maxLight) maxLight = light;
                }
            }
        }

        Vec3 result = null;
        for (Map.Entry<Vec3, Integer> e : brightMap.entrySet()) {
            if (e.getValue() == maxLight) {
                if (result == null) result = e.getKey();
                else { result = null; break; } // Empate, luz ambiental
            }
        }

        if (result == null) {
            result = new Vec3(0.2, 0.8, 0.0);
        } else {
            result = new Vec3(result.x, result.y, -result.z);
            float yawDelta = -MathUtil.lerpYaw(entity.yBodyRot, entity.yBodyRotO, partial);
            result = VectorMathUtil.rotateAroundY(result, yawDelta);
        }
        return result.normalize();
    }

    // ── Escaneo de Superficie (Suelo) ────────────────────────────────────────

    /** Usa el Heightmap del motor para encontrar el suelo de forma eficiente. */
    public static int getSolidSurface(Level level, int x, int z) {
        // WORLD_SURFACE ignora fluidos, MOTION_BLOCKING incluye todo lo que detiene el paso
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }

    public static BlockPos getSolidSurfacePos(Level level, BlockPos pos) {
        return new BlockPos(pos.getX(), getSolidSurface(level, pos.getX(), pos.getZ()), pos.getZ());
    }

    // ── Detección de Camas (Modernizada) ─────────────────────────────────────

    public static boolean isBed(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // En 1.20.1, BedBlock es la clase base para todas las camas (incluso de mods si heredan bien)
        if (state.is(Blocks.AIR)) return false;
        if (state.getBlock() instanceof BedBlock) return true;

        // Fallback para camas de mods extraños que usan nombres en el TileEntity
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null) {
            String name = be.getType().toString().toLowerCase(); // Usamos el ID de registro
            return name.contains("bed");
        }
        return false;
    }

    // ── Efectos Visuales ─────────────────────────────────────────────────────

    /** Crea un anillo de partículas alrededor de un punto (ej: al spawnear o teletransportarse) */
    public static <P extends ParticleOptions> void spawnRingParticles(Level level, P particleType, Vec3 pos, int count, double radius, double maxVelY) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * ((double) i / count);
            double vx = Math.sin(angle) * radius;
            double vz = Math.cos(angle) * radius;
            level.addParticle(particleType,
                    pos.x + vx, pos.y, pos.z + vz,
                    0.0D, ModConstants.RANDOM.nextDouble() * maxVelY, 0.0D);
        }
    }

    // ── Lógica de Camas (Cabecera vs Pies) ───────────────────────────────────

    public static BlockPos getBedHeadPos(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BedBlock)) return null;

        Direction facing = state.getValue(BedBlock.FACING);
        BedPart part = state.getValue(BedBlock.PART);

        // Si estamos en los pies, la cabecera está hacia donde mira la cama.
        // Si ya estamos en la cabecera, devolvemos esta misma posición.
        return (part == BedPart.FOOT) ? pos.relative(facing) : pos;
    }

    // ── Rastreo de Jugadores (Sincronización) ────────────────────────────────

    /** Devuelve los jugadores que están cerca y "viendo" a esta entidad (Servidor). */
    public static Collection<ServerPlayer> getTrackingPlayers(Entity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel sl)) return Collections.emptyList();

        // El ChunkMap de la 1.20.1 gestiona quién ve qué de forma muy eficiente
        return sl.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(entity.blockPosition()), false);
    }
}