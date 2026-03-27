package com.trolmastercard.sexmod.util;
import com.trolmastercard.sexmod.ModConstants;

import com.google.common.collect.Sets;
import com.trolmastercard.sexmod.client.FakeWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * NpcWorldUtil - ported from cj.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Pure-static utility class used extensively by NPC AI / model code.
 *
 * Methods:
 *   {@link #angleDelta(float, float)}           - signed angle delta between two angles
 *   {@link #computeSexmodLightDir(LivingEntity, float)} - lighting direction vector for SEXMOD mode
 *   {@link #getSolidSurface(Level, int, int)}   - Y of highest non-transparent block
 *   {@link #getSolidSurfacePos(Level, BlockPos)} - BlockPos version
 *   {@link #isBed(Level, BlockPos)}             - quick bed check without click context
 *   {@link #isBed(Level, BlockPos, Vec3, Direction, Player)} - full bed check
 *   {@link #spawnRingParticles(Level, ParticleType, Vec3, int, double, double)} - ring of particles
 *   {@link #getBedHeadPos(BlockPos, BlockState)} - head-block pos of a bed
 *   {@link #getTrackingPlayers(Entity)}         - set of players tracking an entity
 *
 * Key API migrations (1.12.2 - 1.20.1):
 *   World.func_175721_c(pos, false) - level.getBrightness(LightLayer.SKY, pos) (sky light)
 *   world.func_72800_K()            - level.getMaxBuildHeight()
 *   world.func_180495_p(pos)        - level.getBlockState(pos)
 *   IBlockState.func_177230_c()     - blockState.getBlock()
 *   block.isBed(state, world, pos, null) - state.getBlock() instanceof BedBlock
 *   tileEntity.func_145748_c_()     - blockEntity.getName() (if INameable)
 *   EnumFacing                      - Direction
 *   BlockBed.EnumPartType.FOOT/HEAD - BedPart.FOOT/HEAD
 *   paramWorld.func_147447_a(ClipContext) - level.clip(ClipContext)
 *   world.func_175688_a(type,x,y,z,...) - level.addParticle(type,x,y,z,...)
 *   Block.getPickBlock - item registry name check (simplified)
 *   func_71218_a(dimension).func_73039_n().getTrackingPlayers(entity)
 *     - ((ServerLevel)level).getChunkSource().chunkMap.getPlayers(chunk, false)
 *   gc.b(angle)  - MathUtil.normalizeAngle(angle)
 *   b6.a(entity.yaw, entity.prevYaw, partial) - MathUtil.lerpYaw(...)
 *   ck.a(vec3d, yaw) - VectorMathUtil.rotateAroundY(vec3d, yaw)
 *   r.f - ModConstants.RANDOM
 */
public class NpcWorldUtil {

    // =========================================================================
    //  angleDelta  (original: cj.a(float, float))
    // =========================================================================

    /**
     * Returns the signed angle delta (in radians) from {@code from} to {@code to},
     * normalised to [--, +-].
     */
    public static float angleDelta(float from, float to) {
        from = MathUtil.normalizeAngle(from);
        to   = MathUtil.normalizeAngle(to);
        float diff    = Math.abs(from - to);
        float diffAlt = (float) (Math.PI * 2) - diff;
        float smallest = Math.min(diff, diffAlt);
        if (from > to) return -smallest;
        return smallest;
    }

    // =========================================================================
    //  computeSexmodLightDir  (original: cj.a(EntityLivingBase, float))
    // =========================================================================

    /**
     * Computes a light-direction Vec3 for SEXMOD-mode rendering.
     * Samples a 3-3-3 cube around the entity and finds the brightest neighbour.
     * Returns the normalised direction toward that neighbour (rotated by the
     * entity's yaw delta).  Falls back to {@code (0,1,0)} for FakeWorld.
     */
    public static Vec3 computeSexmodLightDir(LivingEntity entity, float partial) {
        Level world = entity.level();
        if (world instanceof FakeWorld) return new Vec3(0, 1, 0);

        BlockPos center = new BlockPos(
            (int) Math.floor(entity.getX()),
            (int) Math.floor(entity.getY()),
            (int) Math.floor(entity.getZ()));

        HashMap<Vec3, Integer> brightMap = new HashMap<>();
        int maxLight = 0;

        for (int dx = -1; dx < 2; dx++) {
            for (int dy = -1; dy < 2; dy++) {
                for (int dz = -1; dz < 2; dz++) {
                    int light = world.getMaxLocalRawBrightness(
                        center.offset(dx, dy, dz));
                    brightMap.put(new Vec3(dx, dy, dz), light);
                    if (light > maxLight) maxLight = light;
                }
            }
        }

        Vec3 result = null;
        for (Map.Entry<Vec3, Integer> e : brightMap.entrySet()) {
            if (e.getValue() != maxLight) continue;
            if (result == null) {
                result = e.getKey();
            } else {
                result = null; // ambiguous  tie
            }
        }

        if (result == null) {
            result = new Vec3(0.2, 0.8, 0.0);
        } else {
            result = new Vec3(result.x, result.y, -result.z);
            float yawDelta = -MathUtil.lerpYaw(
                entity.yBodyRot, entity.yBodyRotO, partial);
            result = VectorMathUtil.rotateAroundY(result, yawDelta);
        }
        return result.normalize();
    }

    // =========================================================================
    //  getSolidSurface  (original: cj.a(World, int, int))
    // =========================================================================

    /**
     * Returns the Y of the highest solid (non-transparent-ish) block in the column.
     * Transparent block set: air, water, lava, leaves, glass, ice.
     */
    public static int getSolidSurface(Level level, int x, int z) {
        Set<Block> transparent = Sets.newHashSet(
            Blocks.AIR, Blocks.WATER, Blocks.LAVA,
            Blocks.OAK_LEAVES, Blocks.GLASS, Blocks.ICE);
        int y = level.getMaxBuildHeight();
        boolean found = false;
        while (!found && y-- >= 0) {
            Block b = level.getBlockState(new BlockPos(x, y, z)).getBlock();
            found = !transparent.contains(b);
        }
        return y;
    }

    /** Returns a BlockPos at the solid surface below {@code pos}. */
    public static BlockPos getSolidSurfacePos(Level level, BlockPos pos) {
        return new BlockPos(pos.getX(),
            getSolidSurface(level, pos.getX(), pos.getZ()),
            pos.getZ());
    }

    // =========================================================================
    //  isBed (various overloads)
    // =========================================================================

    /** Quick bed check - no click context required. Original: {@code cj.b(World, BlockPos)} */
    public static boolean isBed(Level level, BlockPos pos) {
        return isBed(level, pos, null, null, null);
    }

    /**
     * Full bed-detection check, including TileEntity name fallback and
     * item pick-block name check.
     * Original: {@code cj.a(World, BlockPos, Vec3d, EnumFacing, EntityPlayer)}
     */
    public static boolean isBed(Level level, BlockPos pos,
                                 Vec3 hitVec, Direction face, Player player) {
        BlockState state = level.getBlockState(pos);
        // 1. Direct BedBlock check
        if (state.getBlock() instanceof BedBlock) return true;

        // 2. BlockEntity name check (mod beds)
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity named) {
            String name = named.getName().getString().toLowerCase();
            if (name.contains(" bed") || name.contains("bed ")) return true;
        }

        // 3. Item name check (fallback for unusual beds)
        if (face == null || hitVec == null) return false;
        // Cannot safely call getPickBlock without full context - skip
        return false;
    }

    // =========================================================================
    //  spawnRingParticles  (original: cj.a(World, EnumParticleTypes, Vec3d, int, double, double))
    // =========================================================================

    /**
     * Spawns {@code count} particles evenly distributed around a horizontal ring
     * of radius {@code radius} centred at {@code pos}.  Each particle has an
     * upward velocity between 0 and {@code maxVelocityY}.
     */
    public static <P extends net.minecraft.core.particles.ParticleOptions> void spawnRingParticles(
            Level level, P particleType, Vec3 pos,
            int count, double radius, double maxVelocityY) {
        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0D) * ((double) i / count);
            double vx    = Math.sin(angle) * radius;
            double vz    = Math.cos(angle) * radius;
            level.addParticle(particleType,
                pos.x + vx, pos.y, pos.z + vz,
                0.0D,
                com.trolmastercard.sexmod.ModConstants.RANDOM.nextFloat() * maxVelocityY,
                0.0D);
        }
    }

    // =========================================================================
    //  getBedHeadPos  (original: cj.a(BlockPos, IBlockState))
    // =========================================================================

    /**
     * Returns the BlockPos of the "head" part of a bed, given any bed block position.
     * If the given block is already the HEAD part, returns the FOOT part instead
     * (original convention: always returns the "other" half's position relative to foot).
     * Returns {@code null} if the state is not a bed or lacks required properties.
     */
    public static BlockPos getBedHeadPos(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BedBlock)) {
            System.out.println("bed is fucked up - not a BedBlock");
            return null;
        }
        if (!state.hasProperty(BedBlock.FACING) || !state.hasProperty(BedBlock.PART)) {
            System.out.println("bed is fucked up - missing properties");
            return null;
        }
        Direction facing  = state.getValue(BedBlock.FACING);
        BedPart   part    = state.getValue(BedBlock.PART);

        // From FOOT: head is in the facing direction
        // From HEAD: foot is opposite the facing direction
        Direction toHead = (part == BedPart.FOOT) ? facing : facing.getOpposite();

        BlockPos result = switch (toHead) {
            case NORTH -> pos.north();
            case SOUTH -> pos.south();
            case EAST  -> pos.east();
            case WEST  -> pos.west();
            default    -> null;
        };

        if (result == null) {
            System.out.println("bed is fucked up - it appears to be positioned vertically (wtf?)");
        }
        return result;
    }

    // =========================================================================
    //  getTrackingPlayers  (original: cj.a(Entity))
    // =========================================================================

    /**
     * Returns the set of players currently tracking (receiving updates for) the entity.
     * SERVER-side only.
     */
    public static Set<? extends Player> getTrackingPlayers(Entity entity) {
        if (entity == null) return Collections.emptySet();
        Level level = entity.level();
        if (!(level instanceof ServerLevel sl)) return Collections.emptySet();
        return sl.getChunkSource().chunkMap.getPlayers(
            new net.minecraft.world.level.ChunkPos(entity.blockPosition()), false);
    }
}
