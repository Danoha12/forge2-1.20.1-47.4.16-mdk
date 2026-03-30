package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

/**
 * PathUtil — Portado a 1.20.1.
 * * Utilidades matemáticas y de navegación para el Pathfinding de los NPCs.
 * * Optimizada para no generar Garbage Collection (lag) durante la búsqueda.
 */
public class PathUtil {

    /** * Devuelve el bloque destino actual de un Path, o BlockPos.ZERO si es nulo.
     */
    public static BlockPos getPathTarget(Path path) {
        if (path == null) return BlockPos.ZERO;

        Node endpoint = path.getEndNode();
        if (endpoint == null) return BlockPos.ZERO;

        // Magia de 1.20.1: asBlockPos() hace el trabajo por ti
        return endpoint.asBlockPos();
    }

    /** * Devuelve el destino de navegación actual de un Mob, o BlockPos.ZERO.
     */
    public static BlockPos getPathTarget(Mob mob) {
        if (mob == null || mob.getNavigation() == null) return BlockPos.ZERO;
        return getPathTarget(mob.getNavigation().getPath());
    }

    /**
     * Devuelve true si algún nodo en el Path dado coincide con alguna de las posiciones.
     * 🚨 Ultra-optimizado: Sin ArrayLists temporales, lectura directa.
     */
    public static boolean pathContainsAny(Path path, BlockPos[] positions) {
        if (path == null || positions == null || positions.length == 0) return false;

        int nodeCount = path.getNodeCount();

        // Leemos los nodos directamente sin crear listas en memoria
        for (int i = 0; i < nodeCount; i++) {
            BlockPos nodePos = path.getNode(i).asBlockPos();

            for (BlockPos pos : positions) {
                // equals() en BlockPos es nativo, rápido y seguro
                if (nodePos.equals(pos)) {
                    return true;
                }
            }
        }
        return false;
    }
}