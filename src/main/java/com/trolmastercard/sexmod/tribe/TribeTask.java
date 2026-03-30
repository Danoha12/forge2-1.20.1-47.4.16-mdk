package com.trolmastercard.sexmod.tribe;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TribeTask — Portado a 1.20.1.
 * * Tareas físicas asignadas a los Kobolds (Talar, Minar).
 */
public class TribeTask {

    public static final int TREE_SCAN_HEIGHT = 30;

    private final BlockPos anchorPos;
    private final Kind kind;
    private final Set<BlockPos> targetBlocks;

    // Usamos CopyOnWriteArrayList para evitar ConcurrentModificationException
    // si un Kobold muere o es removido mientras la tribu itera sobre los trabajadores.
    private final List<KoboldEntity> workers = new CopyOnWriteArrayList<>();

    private Direction facing = Direction.NORTH;

    public TribeTask(BlockPos anchorPos, Kind kind, Set<BlockPos> targetBlocks) {
        this.anchorPos = anchorPos;
        this.kind = kind;
        this.targetBlocks = targetBlocks;
    }

    public TribeTask(BlockPos anchorPos, Kind kind, Set<BlockPos> targetBlocks, Direction facing) {
        this(anchorPos, kind, targetBlocks);
        this.facing = facing;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public BlockPos getTargetPos() { return anchorPos; }
    public Kind getKind() { return kind; }
    public Set<BlockPos> getTargetBlocks() { return targetBlocks; }
    public Direction getFacing() { return facing; }
    public List<KoboldEntity> getWorkers() { return workers; }

    // ── Gestión de Trabajadores ──────────────────────────────────────────────

    public boolean hasWorker(KoboldEntity kobold) {
        return workers.contains(kobold);
    }

    public boolean assignWorker(KoboldEntity kobold) {
        if (workers.size() >= kind.maxWorkers) return false;
        if (!workers.contains(kobold)) {
            workers.add(kobold);
        }
        return true;
    }

    public void removeWorker(KoboldEntity kobold) {
        workers.remove(kobold);
    }

    public boolean isFull() {
        return workers.size() >= kind.maxWorkers;
    }

    public void onMemberRemoved(KoboldEntity kobold) {
        removeWorker(kobold);
    }

    /** * Libera a todos los trabajadores y resetea su estado físico.
     * Fundamental para que no se queden flotando si el árbol se destruye de golpe.
     */
    public void dismissAll() {
        for (KoboldEntity k : workers) {
            try {
                if (!k.isInteractiveModeActive()) { // Reemplaza la comprobación de getCurrentSexPartner
                    k.setNoGravity(false);
                    k.noPhysics = false;
                    k.setAnimState(AnimState.NULL);
                    k.getEntityData().set(BaseNpcEntity.DATA_FROZEN, false);
                }
            } catch (Exception e) {
                System.err.println("[SexMod] Error al liberar Kobold de la tarea: " + e.getMessage());
            }
        }
        workers.clear();
    }

    // ── Escáner de Árboles (Flood Fill) ──────────────────────────────────────

    public static Set<BlockPos> buildTreeTask(Level level, BlockPos startPos, UUID tribeId) {
        BlockPos bottom = startPos;

        // 1. Encontrar la base real del tronco (por si picaron un bloque del medio)
        while (isLog(level, bottom.below())) {
            bottom = bottom.below();
        }

        // 2. Encontrar la cima del tronco principal
        BlockPos top = bottom;
        while (isLog(level, top.above()) && (top.getY() - bottom.getY()) < TREE_SCAN_HEIGHT) {
            top = top.above();
        }

        Set<BlockPos> result = new HashSet<>();

        // 3. Añadir la columna principal para garantizar la estructura base
        for (int y = bottom.getY(); y <= top.getY(); y++) {
            result.add(new BlockPos(bottom.getX(), y, bottom.getZ()));
        }

        // 4. BFS (Flood Fill) para absorber las ramas
        result.addAll(floodFillLogs(level, bottom));

        // 5. Filtrar bloques que ya estén reclamados por otra tarea de esta tribu
        TribeData data = TribeManager.getTribe(tribeId); // Asumiendo que añadiste este getter
        if (data != null) {
            Set<TribeTask> currentTasks = data.getTasks();
            Set<BlockPos> claimed = new HashSet<>();

            for (TribeTask task : currentTasks) {
                // Intersección de conjuntos más rápida
                for (BlockPos bp : result) {
                    if (task.getTargetBlocks().contains(bp)) {
                        claimed.add(bp);
                    }
                }
            }
            result.removeAll(claimed);
        }

        // 6. Registrar la nueva tarea si quedan bloques
        if (!result.isEmpty()) {
            TribeTask task = new TribeTask(bottom, Kind.FALL_TREE, result);
            if (data != null) {
                data.addTask(task);
                TribeManager.markDirty();
            }
        }

        return result;
    }

    private static boolean isLog(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(BlockTags.LOGS);
    }

    /** Flood-fill iterativo con límite de seguridad (500 bloques). */
    private static Set<BlockPos> floodFillLogs(Level level, BlockPos start) {
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>(); // ArrayDeque es más rápido que LinkedList

        queue.add(start);
        visited.add(start);

        // Direcciones de expansión (adyacentes y diagonales 3D cercanas)
        int[][] offsets = {
                {1,0,0}, {-1,0,0}, {0,0,1}, {0,0,-1},
                {1,0,1}, {-1,0,-1}, {-1,0,1}, {1,0,-1},
                {0,1,0},
                {1,1,0}, {-1,1,0}, {0,1,1}, {0,1,-1},
                {1,1,1}, {-1,1,-1}, {-1,1,1}, {1,1,-1}
        };

        while (!queue.isEmpty() && visited.size() < 500) {
            BlockPos current = queue.poll();

            for (int[] o : offsets) {
                BlockPos next = current.offset(o[0], o[1], o[2]);
                if (!visited.contains(next) && isLog(level, next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        return visited;
    }

    // ── Enumeración de Tipos ─────────────────────────────────────────────────

    public enum Kind {
        FALL_TREE(1),
        MINE(3);

        public final int maxWorkers;

        Kind(int maxWorkers) {
            this.maxWorkers = maxWorkers;
        }
    }
}