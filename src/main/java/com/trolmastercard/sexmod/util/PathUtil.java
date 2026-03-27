package com.trolmastercard.sexmod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.Node;

import java.util.ArrayList;

/**
 * Utility helpers for pathfinding queries.
 * Obfuscated name: fl
 */
public class PathUtil {

    /** Returns the current destination block of a path, or BlockPos.ZERO if null. */
    public static BlockPos getPathTarget(Path path) {
        if (path == null) return BlockPos.ZERO;
        Node endpoint = path.getEndNode();
        if (endpoint == null) return BlockPos.ZERO;
        return new BlockPos(endpoint.x, endpoint.y, endpoint.z);
    }

    /** Returns the current navigation destination of a mob, or BlockPos.ZERO. */
    public static BlockPos getPathTarget(Mob mob) {
        Path path = mob.getNavigation().getPath();
        return getPathTarget(path);
    }

    /**
     * Returns true if any node in the given path matches one of the provided positions.
     */
    public static boolean pathContainsAny(Path path, BlockPos[] positions) {
        int nodeCount = path.getNodeCount();
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(path.getNode(i));
        }
        for (Node node : nodes) {
            for (BlockPos pos : positions) {
                if (node.x == pos.getX() && node.y == pos.getY() && node.z == pos.getZ()) {
                    return true;
                }
            }
        }
        return false;
    }
}
