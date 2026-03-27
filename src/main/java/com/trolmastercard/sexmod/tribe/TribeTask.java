package com.trolmastercard.sexmod.tribe;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * TribeTask - ported from bs.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Represents a single task assigned to kobolds in a tribe.
 * Tracks the task's location, type, the set of BlockPos positions it covers,
 * and the list of kobolds currently working on it.
 *
 * Inner enum {@link Kind} (original {@code bs.a}):
 *   FALL_TREE(1) - felling a tree, max 1 kobold worker
 *   MINE(3)      - mining, max 3 kobold workers
 *
 * Field mapping:
 *   a = anchorPos   (BlockPos - base position of the task)
 *   c = kind        (Kind enum)
 *   b = positions   (HashSet<BlockPos> - all block positions in scope)
 *   f = members     (List<KoboldEntity> - kobolds assigned)
 *   e = facing      (Direction - facing dir, default NORTH)
 *
 * Static constants:
 *   d = 30  - TREE_SCAN_HEIGHT (max height delta when scanning a tree)
 *
 * In 1.12.2:
 *   - {@code EnumFacing} - {@link Direction}
 *   - {@code BlockLog instanceof} - {@code blockState.is(BlockTags.LOGS)}
 *   - {@code Material.field_151579_a} (air material) - {@code block.isAir(level, pos)}
 *     or simply checking if the block is not a log
 *   - {@code blockPos.func_177982_a(dx,dy,dz)} - {@code blockPos.offset(dx,dy,dz)}
 *   - {@code blockPos.func_177984_a()} - {@code blockPos.above()}
 *   - {@code blockPos.func_177977_b()} - {@code blockPos.below()}
 *   - {@code blockPos.func_177958_n/956_o/952_p()} - {@code blockPos.getX/Y/Z()}
 *   - {@code ax.b(uuid, task)} - {@link TribeManager#addTask(UUID, TribeTask)}
 *   - {@code ax.p(uuid)} - {@link TribeManager#getTasksForTribe(UUID)}
 *   - {@code em.G} - {@code BaseNpcEntity.FROZEN} DataParameter
 *   - {@code em.field_184212_Q().func_187227_b(em.G, false)} -
 *     {@code npc.getEntityData().set(BaseNpcEntity.FROZEN, false)}
 *   - {@code ff.ae()} - {@code kobold.getSexPartner()}
 *   - {@code ff.b(fp.NULL)} - {@code kobold.setAnimState(AnimState.NULL)}
 */
public class TribeTask {

    public static final int TREE_SCAN_HEIGHT = 30;

    private final BlockPos anchorPos;
    private final Kind     kind;
    private final HashSet<BlockPos> positions;
    private final List<KoboldEntity> members = new ArrayList<>();
    private Direction facing = Direction.NORTH;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public TribeTask(BlockPos anchorPos, Kind kind, HashSet<BlockPos> positions) {
        this.anchorPos = anchorPos;
        this.kind      = kind;
        this.positions = positions;
    }

    public TribeTask(BlockPos anchorPos, Kind kind, HashSet<BlockPos> positions,
                     Direction facing) {
        this(anchorPos, kind, positions);
        this.facing = facing;
    }

    // =========================================================================
    //  Accessors
    // =========================================================================

    public Direction getFacing()       { return facing;    }
    public BlockPos  getAnchorPos()    { return anchorPos; }
    public Kind      getKind()         { return kind;      }
    public HashSet<BlockPos> getPositions() { return positions; }
    public List<KoboldEntity> getMembers() { return members; }

    // =========================================================================
    //  Position management
    // =========================================================================

    public void addPosition(BlockPos pos)               { positions.add(pos); }
    public void addPositions(HashSet<BlockPos> set)     { positions.addAll(set); }
    public void removePosition(BlockPos pos)            { positions.remove(pos); }
    public void removePositions(HashSet<BlockPos> set)  { if (!set.isEmpty()) positions.removeAll(set); }
    public boolean containsPosition(BlockPos pos)       { return positions.contains(pos); }

    // =========================================================================
    //  Member management
    // =========================================================================

    /**
     * Attempts to assign a kobold to this task.
     * Returns false if the task already has its maximum number of workers.
     *
     * Original: {@code bs.a(ff)}
     */
    public boolean tryAssign(KoboldEntity kobold) {
        if (kind.maxWorkers <= members.size()) return false;
        members.add(kobold);
        return true;
    }

    /** Removes a kobold from the worker list. Original: {@code bs.c(ff)} */
    public void unassign(KoboldEntity kobold) { members.remove(kobold); }

    /** Returns true if the task is at maximum capacity. Original: {@code bs.e()} */
    public boolean isFull() { return kind.maxWorkers <= members.size(); }

    /** Returns true if the given kobold is assigned. Original: {@code bs.b(ff)} */
    public boolean isAssigned(KoboldEntity kobold) { return members.contains(kobold); }

    /**
     * Dismisses all assigned kobolds and clears the member list.
     * Resets each kobold's frozen/animation state.
     *
     * Original: {@code bs.a()} (void, no params)
     */
    public void dismissAll() {
        for (KoboldEntity kobold : members) {
            try {
                if (kobold.getSexPartner() == null) {
                    kobold.setNoPhysics(false);
                    kobold.noPhysics = false;
                    kobold.setAnimStateNull();           // ff.b(fp.NULL)
                    kobold.getEntityData().set(
                        com.trolmastercard.sexmod.entity.BaseNpcEntity.FROZEN, false);
                }
            } catch (RuntimeException ignored) {}
        }
        members.clear();
    }

    // =========================================================================
    //  Static: build a FALL_TREE task for the tree at the given position
    //  Original: bs.a(World, BlockPos, UUID)
    // =========================================================================

    /**
     * Finds the full extent of the tree whose trunk contains {@code pos},
     * deduplicates positions already claimed by other tasks of the same tribe,
     * registers the new task with {@link TribeManager}, and returns the final
     * set of block positions.
     *
     * Algorithm:
     *  1. Scan downward to find the bottom of the trunk.
     *  2. Scan upward to find the top log block.
     *  3. Collect a vertical column from bottom to top.
     *  4. BFS outward on each Y level to collect all connected log blocks.
     *  5. Exclude the trunk's X/Z column from the BFS result (trunk handled by column).
     *  6. Remove any positions already owned by another task.
     *  7. Register as a new FALL_TREE task.
     */
    public static HashSet<BlockPos> buildTreeTask(Level level, BlockPos pos, UUID tribeId) {
        // Find bottom log
        BlockPos bottom = pos;
        while (!isTrunkBottom(level, bottom)) bottom = bottom.below();

        // Find top log
        BlockPos top = pos;
        while (isLog(level, top.above())) top = top.above();

        // Vertical column
        HashSet<BlockPos> result = new HashSet<>();
        int height = top.getY() - bottom.getY();
        for (int i = 0; i <= height; i++) result.add(bottom.offset(0, i, 0));

        // BFS canopy (connected logs at each level)
        HashSet<BlockPos> canopy = floodFillLogs(level, bottom);

        // Remove trunk column from canopy
        HashSet<BlockPos> trunk = new HashSet<>();
        for (BlockPos bp : canopy) {
            if (bp.getX() == bottom.getX() && bp.getZ() == bottom.getZ()) trunk.add(bp);
        }
        canopy.removeAll(trunk);
        result.addAll(canopy);

        // Remove positions claimed by other tasks
        HashSet<BlockPos> claimed = new HashSet<>();
        for (TribeTask task : TribeManager.getTasksForTribe(tribeId)) {
            HashSet<BlockPos> taskPositions = task.getPositions();
            for (BlockPos bp : result) {
                if (taskPositions.contains(bp)) { claimed.add(bp); break; }
            }
        }
        result.removeAll(claimed);

        TribeTask task = new TribeTask(bottom, Kind.FALL_TREE, result);
        TribeManager.addTask(tribeId, task);
        return result;
    }

    /** True if the block above pos is not a log (= top of trunk). */
    static boolean isTrunkTop(Level level, BlockPos pos) {
        return !isLog(level, pos.above());
    }

    /** True if the block below pos is not a log AND not non-solid (= bottom of trunk). */
    static boolean isTrunkBottom(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (isLog(level, pos.below())) return false;
        // Air or non-solid below - we're at the bottom
        if (!below.getMaterial().isSolid()) return false;
        return true;
    }

    static boolean isLog(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LOGS);
    }

    /**
     * BFS flood-fill of all connected log blocks starting from {@code start}.
     * Searches 8-connected horizontal neighbours and one block upward.
     *
     * Original: {@code bs.a(World, BlockPos)} and {@code bs.a(World, BlockPos, HashSet)}
     */
    static HashSet<BlockPos> floodFillLogs(Level level, BlockPos start) {
        return floodFillLogs(level, start, new HashSet<>());
    }

    static HashSet<BlockPos> floodFillLogs(Level level, BlockPos pos, HashSet<BlockPos> visited) {
        if (visited.contains(pos)) return new HashSet<>();
        visited.add(pos);

        int[][] offsets = {
            {1,0,0},{-1,0,0},{0,0,1},{0,0,-1},
            {1,0,1},{-1,0,-1},{-1,0,1},{1,0,-1},
            {0,1,0},
            {1,1,0},{-1,1,0},{0,1,1},{0,1,-1},
            {1,1,1},{-1,1,-1},{-1,1,1},{1,1,-1}
        };

        for (int[] o : offsets) {
            BlockPos next = pos.offset(o[0], o[1], o[2]);
            if (isLog(level, next)) visited.addAll(floodFillLogs(level, next, visited));
        }
        return visited;
    }

    // =========================================================================
    //  Inner enum: Kind (original bs.a)
    // =========================================================================

    public enum Kind {
        FALL_TREE(1),
        MINE(3);

        /** Maximum number of kobold workers for this task kind. */
        public final int maxWorkers;

        Kind(int maxWorkers) {
            this.maxWorkers = maxWorkers;
        }
    }
}
