package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GalathEntityRegistry — Portado a 1.20.1.
 * * Registro en el lado del cliente (y servidor local) que mapea los UUIDs de los NPCs
 * con sus instancias vivas para búsquedas ultra rápidas.
 * * También almacena los puntos de aparición (Spawn Points) para evitar
 * que múltiples Galaths aparezcan en el mismo lugar.
 */
public class GalathEntityRegistry {

    // Usamos ConcurrentHashMap para evitar crasheos si la red y el cliente leen/escriben al mismo tiempo
    private static final ConcurrentHashMap<UUID, BaseNpcEntity> REGISTRY = new ConcurrentHashMap<>();

    // Set seguro para hilos para almacenar los puntos de aparición (requerido por GalathEntity)
    public static final Set<BlockPos> GALATH_SPAWN_POINTS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // ── Registro de Entidades (UUID -> Instancia) ────────────────────────────

    /** Registra un NPC en el diccionario local. */
    public static void register(BaseNpcEntity npc) {
        if (npc != null && npc.getNpcUUID() != null) {
            REGISTRY.put(npc.getNpcUUID(), npc);
        }
    }

    /** Elimina un NPC del diccionario local. */
    public static void unregister(BaseNpcEntity npc) {
        if (npc != null && npc.getNpcUUID() != null) {
            REGISTRY.remove(npc.getNpcUUID());
        }
    }

    /** Recupera un NPC basado en su UUID (Ultra rápido, O(1)). */
    public static BaseNpcEntity get(UUID uuid) {
        if (uuid == null) return null;
        return REGISTRY.get(uuid);
    }

    // ── Manejo de Puntos de Aparición (Spawn Points) ─────────────────────────

    /** * Registra un bloque donde Galath ya ha aparecido para no sobrepoblar.
     * (Usado en el evento de LivingSpawnEvent de GalathEntity)
     */
    public static void addSpawnPoint(BlockPos pos, Set<BlockPos> targetList) {
        if (pos != null && targetList != null) {
            targetList.add(pos);
        }
    }

    /** Devuelve la lista de todos los puntos de aparición registrados. */
    public static Set<BlockPos> getAllSpawnPoints() {
        return GALATH_SPAWN_POINTS;
    }

    // ── Limpieza General ──────────────────────────────────────────────────────

    /** * Limpia todas las entradas.
     * VITAL: Debe llamarse en los eventos de carga/descarga de mundos (WorldUnloadEvent)
     * para evitar fugas de memoria masivas.
     */
    public static void clear() {
        REGISTRY.clear();
        GALATH_SPAWN_POINTS.clear();
    }
}