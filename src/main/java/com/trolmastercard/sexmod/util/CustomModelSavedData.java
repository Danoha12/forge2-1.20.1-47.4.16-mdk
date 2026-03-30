package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.data.GalathOwnershipData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CustomModelSavedData — Portado a 1.20.1.
 * * Guarda los nombres de modelos personalizados para Galath y MangleLie.
 * * Usa el sistema de SavedData nativo de Minecraft para persistencia en el world/data.
 */
public class CustomModelSavedData extends SavedData {

    public static final String KEY = "sexmod_custom_models";

    private final Map<UUID, String> galathModels = new HashMap<>();
    private final Map<UUID, String> mangModels = new HashMap<>();

    public CustomModelSavedData() {}

    // ── Lógica de NBT (Carga y Guardado) ──────────────────────────────────────

    public static CustomModelSavedData load(CompoundTag tag) {
        CustomModelSavedData data = new CustomModelSavedData();
        loadMap(tag.getCompound("galath"), data.galathModels);
        loadMap(tag.getCompound("mang"), data.mangModels);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.put("galath", saveMap(this.galathModels));
        tag.put("mang", saveMap(this.mangModels));
        return tag;
    }

    // ── Acceso Global ─────────────────────────────────────────────────────────

    public static CustomModelSavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                CustomModelSavedData::load,
                CustomModelSavedData::new,
                KEY
        );
    }

    // ── API de Modelos ────────────────────────────────────────────────────────

    public String getModelName(BaseNpcEntity npc) {
        if (npc instanceof GalathEntity) {
            UUID id = npc.getMasterUUID();
            UUID owner = GalathOwnershipData.getPlayerFor(id);
            return galathModels.getOrDefault(owner != null ? owner : id, "");
        }
        if (npc instanceof MangleLieEntity mang) {
            UUID seatMaster = mang.getSeatMasterUUID();
            UUID owner = seatMaster != null ? GalathOwnershipData.getPlayerFor(seatMaster) : null;
            return mangModels.getOrDefault(owner != null ? owner : npc.getMasterUUID(), "");
        }
        return "";
    }

    public void saveModelName(BaseNpcEntity npc) {
        String modelName = npc.getCustomModelName();
        if (npc instanceof GalathEntity) {
            UUID id = npc.getMasterUUID();
            UUID owner = GalathOwnershipData.getPlayerFor(id);
            galathModels.put(owner != null ? owner : id, modelName);
            this.setDirty();
        } else if (npc instanceof MangleLieEntity mang) {
            UUID seatMaster = mang.getSeatMasterUUID();
            UUID owner = seatMaster != null ? GalathOwnershipData.getPlayerFor(seatMaster) : null;
            mangModels.put(owner != null ? owner : npc.getMasterUUID(), modelName);
            this.setDirty();
        }
    }

    // ── Helpers de NBT ────────────────────────────────────────────────────────

    private static CompoundTag saveMap(Map<UUID, String> map) {
        CompoundTag tag = new CompoundTag();
        int i = 0;
        for (Map.Entry<UUID, String> entry : map.entrySet()) {
            tag.putString("UUID" + i, entry.getKey().toString());
            tag.putString("MODEL" + i, entry.getValue());
            i++;
        }
        return tag;
    }

    private static void loadMap(CompoundTag tag, Map<UUID, String> map) {
        for (int i = 0; ; i++) {
            String uuidStr = tag.getString("UUID" + i);
            if (uuidStr.isEmpty()) break;
            try {
                map.put(UUID.fromString(uuidStr), tag.getString("MODEL" + i));
            } catch (IllegalArgumentException ignored) {}
        }
    }
}