package com.trolmastercard.sexmod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CustomGirlNamesSavedData - ported from gy.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Persists custom NPC names per (tribe UUID, NpcType) pair across sessions.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - WorldSavedData - SavedData
 *   - func_76184_a(NBTTagCompound) - load(CompoundTag)  [static factory pattern]
 *   - func_189551_b(NBTTagCompound) - save(CompoundTag)
 *   - func_76185_a() - setDirty()
 *   - world.func_175693_T().func_75745_a / func_75742_a - level.getDataStorage().computeIfAbsent / get
 *   - WorldEvent.Save / Load - LevelEvent.Save / Load
 *   - NBTTagCompound.func_150296_c() - CompoundTag.getAllKeys()
 *   - func_74775_l(key) - getCompound(key)
 *   - func_74778_a(key, val) - putString(key, val)
 *   - func_74779_i(key) - getString(key)
 */
public class CustomGirlNamesSavedData extends SavedData {

    public static final String KEY = "sexmod:customstaticgirlnames";

    /** In-memory cache: tribeUUID - (NpcType - customName) */
    static final HashMap<UUID, HashMap<NpcType, String>> data = new HashMap<>();

    // -- SavedData factory ------------------------------------------------------

    public static CustomGirlNamesSavedData load(CompoundTag tag) {
        CustomGirlNamesSavedData instance = new CustomGirlNamesSavedData();
        for (String key : tag.getAllKeys()) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            data.put(uuid, fromNbt(tag.getCompound(key)));
        }
        return instance;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        for (Map.Entry<UUID, HashMap<NpcType, String>> entry : data.entrySet()) {
            tag.put(entry.getKey().toString(), toNbt(entry.getValue()));
        }
        return tag;
    }

    // -- World events -----------------------------------------------------------

    @SubscribeEvent
    public void onSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getDataStorage().set(KEY, this);
            setDirty();
        }
    }

    @SubscribeEvent
    public void onLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getDataStorage().computeIfAbsent(CustomGirlNamesSavedData::load, CustomGirlNamesSavedData::new, KEY);
        }
    }

    // -- Public API -------------------------------------------------------------

    /** Store a custom name for the given (tribe, type) combination. */
    public static void setName(UUID tribeId, NpcType type, String name) {
        data.computeIfAbsent(tribeId, k -> new HashMap<>()).put(type, name);
    }

    /** Retrieve the custom name for (tribe, type), or null if unset. */
    @Nullable
    public static String getName(UUID tribeId, NpcType type) {
        HashMap<NpcType, String> map = data.get(tribeId);
        if (map == null) return null;
        return map.get(type);
    }

    // -- NBT helpers ------------------------------------------------------------

    private static CompoundTag toNbt(HashMap<NpcType, String> map) {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<NpcType, String> e : map.entrySet()) {
            tag.putString(e.getKey().name(), e.getValue());
        }
        return tag;
    }

    private static HashMap<NpcType, String> fromNbt(CompoundTag tag) {
        HashMap<NpcType, String> map = new HashMap<>();
        for (NpcType type : NpcType.values()) {
            String val = tag.getString(type.name());
            if (!"".equals(val)) map.put(type, val);
        }
        return map;
    }
}
