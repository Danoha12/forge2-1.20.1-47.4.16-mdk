package com.trolmastercard.sexmod.data;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.tribe.GalathOwnershipData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CustomModelSavedData - ported from bj.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * {@link SavedData} that persists the custom-model assignments for
 * Galath ({@code c}) and MangleLie ({@code b}) entities across world saves.
 *
 * Each map stores {@code UUID - modelName} pairs. The UUID used as the key
 * follows the same ownership-lookup logic as the original:
 *   - For {@code GalathEntity}: looks up the owner UUID via {@link GalathOwnershipData};
 *     falls back to the NPC's own master UUID if no owner is found.
 *   - For {@code MangleLieEntity}: looks up the owner UUID (via the seat entity's
 *     {@code v()} method); falls back to the NPC's master UUID.
 *
 * NBT layout (unchanged from original):
 * <pre>
 *   "sexmod:static_custom_model_manager" : {
 *     "galath" : { "UUID0": "...", "MODEL0": "...", ... }
 *     "mang"   : { "UUID0": "...", "MODEL0": "...", ... }
 *   }
 * </pre>
 *
 * In 1.12.2:
 *   - Extended {@code WorldSavedData} - now extends {@code SavedData}.
 *   - {@code func_76184_a(NBTTagCompound)} - {@link #load(CompoundTag)}.
 *   - {@code func_189551_b(NBTTagCompound)} - {@link #save(CompoundTag)}.
 *   - {@code WorldEvent.Save} / {@code WorldEvent.Load} removed; SavedData is
 *     automatically saved and loaded by Minecraft when registered.
 *   - {@code world.func_175693_T().func_75745_a/func_75742_a} -
 *     {@code level.getDataStorage().computeIfAbsent(...)}.
 */
@Mod.EventBusSubscriber
public class CustomModelSavedData extends SavedData {

    public static final String KEY = "sexmod_custom_model_manager";

    /** Galath NPC UUID - custom model name. */
    public static HashMap<UUID, String> galathModels  = new HashMap<>();

    /** MangleLie NPC UUID - custom model name. */
    public static HashMap<UUID, String> mangModels    = new HashMap<>();

    // =========================================================================
    //  SavedData factory
    // =========================================================================

    public static CustomModelSavedData create() {
        return new CustomModelSavedData();
    }

    public static CustomModelSavedData load(CompoundTag tag) {
        CustomModelSavedData data = new CustomModelSavedData();
        CompoundTag root = tag.getCompound(KEY);
        loadMap(root.getCompound("galath"), galathModels);
        loadMap(root.getCompound("mang"),   mangModels);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag root = new CompoundTag();
        root.put("galath", saveMap(galathModels));
        root.put("mang",   saveMap(mangModels));
        tag.put(KEY, root);
        return tag;
    }

    // =========================================================================
    //  Read / write model name for a given NPC
    //  Original: bj.c(em) / bj.a(em)
    // =========================================================================

    /** Returns the model name for the given NPC, or "" if not found. */
    public static String getModelName(BaseNpcEntity npc) {
        String name = getModelNameNullable(npc);
        return name != null ? name : "";
    }

    /** Returns the model name for the NPC, or null. */
    private static String getModelNameNullable(BaseNpcEntity npc) {
        if (npc instanceof GalathEntity) {
            UUID npcId  = npc.getMasterUUID();
            UUID ownerId = GalathOwnershipData.getPlayerFor(npcId);
            return galathModels.get(ownerId != null ? ownerId : npcId);
        }
        if (npc instanceof MangleLieEntity mang) {
            UUID seatMaster = mang.getSeatMasterUUID();
            UUID ownerId    = seatMaster != null ? GalathOwnershipData.getPlayerFor(seatMaster) : null;
            return mangModels.get(ownerId != null ? ownerId : npc.getMasterUUID());
        }
        return null;
    }

    /** Saves the model name from the NPC's current custom model index. */
    public static void saveModelName(BaseNpcEntity npc) {
        if (npc instanceof GalathEntity) {
            UUID npcId   = npc.getMasterUUID();
            UUID ownerId = GalathOwnershipData.getPlayerFor(npcId);
            galathModels.put(ownerId != null ? ownerId : npcId, npc.getCustomModelName());
            return;
        }
        if (npc instanceof MangleLieEntity mang) {
            UUID seatMaster = mang.getSeatMasterUUID();
            UUID ownerId    = seatMaster != null ? GalathOwnershipData.getPlayerFor(seatMaster) : null;
            mangModels.put(ownerId != null ? ownerId : npc.getMasterUUID(), npc.getCustomModelName());
        }
    }

    /** Clears both maps (called on reload). Original: {@code bj.a()} */
    public static void clearAll() {
        galathModels.clear();
        mangModels.clear();
    }

    // =========================================================================
    //  NBT helpers
    // =========================================================================

    private static CompoundTag saveMap(HashMap<UUID, String> map) {
        CompoundTag tag = new CompoundTag();
        int i = 0;
        for (Map.Entry<UUID, String> entry : map.entrySet()) {
            tag.putString("UUID"  + i, entry.getKey().toString());
            tag.putString("MODEL" + i, entry.getValue());
            i++;
        }
        return tag;
    }

    private static void loadMap(CompoundTag tag, HashMap<UUID, String> map) {
        for (int i = 0; ; i++) {
            String uuidStr = tag.getString("UUID" + i);
            if (uuidStr.isEmpty()) break;
            map.put(UUID.fromString(uuidStr), tag.getString("MODEL" + i));
        }
    }
}
