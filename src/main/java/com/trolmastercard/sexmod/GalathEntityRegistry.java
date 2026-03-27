package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.BaseNpcEntity;

import java.util.HashMap;
import java.util.UUID;

/**
 * Client-side registry that maps NPC UUIDs to their live entity instances.
 * Obfuscated name: fs
 */
public class GalathEntityRegistry {

    private static final HashMap<UUID, BaseNpcEntity> REGISTRY = new HashMap<>();

    /** Register an NPC into the client registry. */
    public static void register(BaseNpcEntity npc) {
        REGISTRY.put(npc.getNpcUUID(), npc);
    }

    /** Unregister an NPC from the client registry. */
    public static void unregister(BaseNpcEntity npc) {
        REGISTRY.remove(npc.getNpcUUID());
    }

    /** Clear all entries (called on world unload). */
    public static void clear() {
        REGISTRY.clear();
    }

    /** Retrieve an NPC by its UUID. */
    public static BaseNpcEntity get(UUID uuid) {
        return REGISTRY.get(uuid);
    }
}
