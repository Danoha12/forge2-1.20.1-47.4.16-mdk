package com.trolmastercard.sexmod.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CustomGirlNamesSavedData — Portado a 1.20.1.
 * * Guarda y carga los nombres personalizados de los NPCs vinculados a un UUID (Tribu/Jugador).
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CustomGirlNamesSavedData extends SavedData {

    public static final String KEY = "sexmod_npc_names";

    // Mapa interno: UUID -> (Tipo de NPC -> Nombre Personalizado)
    private final Map<UUID, Map<NpcType, String>> nameData = new HashMap<>();

    public CustomGirlNamesSavedData() {}

    // ── Lógica de NBT (Guardado y Carga) ──────────────────────────────────────

    /** Factoría estática para cargar los datos desde el archivo .dat del mundo */
    public static CustomGirlNamesSavedData load(CompoundTag tag) {
        CustomGirlNamesSavedData instance = new CustomGirlNamesSavedData();
        for (String key : tag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag npcTag = tag.getCompound(key);
                Map<NpcType, String> map = new HashMap<>();

                for (NpcType type : NpcType.values()) {
                    if (npcTag.contains(type.name())) {
                        map.put(type, npcTag.getString(type.name()));
                    }
                }
                instance.nameData.put(uuid, map);
            } catch (IllegalArgumentException ignored) {}
        }
        return instance;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<UUID, Map<NpcType, String>> entry : nameData.entrySet()) {
            CompoundTag npcTag = new CompoundTag();
            for (Map.Entry<NpcType, String> e : entry.getValue().entrySet()) {
                npcTag.putString(e.getKey().name(), e.getValue());
            }
            tag.put(entry.getKey().toString(), npcTag);
        }
        return tag;
    }

    // ── Acceso Global (Singleton por Mundo) ──────────────────────────────────

    public static CustomGirlNamesSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                CustomGirlNamesSavedData::load,
                CustomGirlNamesSavedData::new,
                KEY
        );
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public void setName(UUID tribeId, NpcType type, String name) {
        nameData.computeIfAbsent(tribeId, k -> new HashMap<>()).put(type, name);
        this.setDirty(); // ¡Vital! Le dice a Minecraft que debe guardar el archivo en disco
    }

    @Nullable
    public String getName(UUID tribeId, NpcType type) {
        Map<NpcType, String> map = nameData.get(tribeId);
        return (map != null) ? map.get(type) : null;
    }
}