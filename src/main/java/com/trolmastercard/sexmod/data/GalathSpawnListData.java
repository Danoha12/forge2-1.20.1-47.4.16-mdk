package com.trolmastercard.sexmod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists Galath and MangleLie spawn positions across sessions.
 * Obfuscated name: fq
 */
public class GalathSpawnListData extends SavedData {

    private static final String DATA_NAME    = "sexmod:galath_spawn_list";
    private static final String KEY_GALATH   = "";
    private static final String KEY_MANG     = "mang";

    /** Galath (Mommy) spawn positions. */
    public static final List<BlockPos> GALATH_POSITIONS = new ArrayList<>();

    /** MangleLie spawn positions. */
    public static final List<BlockPos> MANG_POSITIONS = new ArrayList<>();

    // -- SavedData lifecycle ---------------------------------------------------

    public GalathSpawnListData() {}

    public static GalathSpawnListData load(CompoundTag tag) {
        GalathSpawnListData data = new GalathSpawnListData();
        CompoundTag inner = tag.getCompound(DATA_NAME);
        readPositions(inner, KEY_GALATH, GALATH_POSITIONS);
        readPositions(inner, KEY_MANG,   MANG_POSITIONS);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag inner = new CompoundTag();
        writePositions(inner, KEY_GALATH, GALATH_POSITIONS);
        writePositions(inner, KEY_MANG,   MANG_POSITIONS);
        tag.put(DATA_NAME, inner);
        return tag;
    }

    // -- Helpers ---------------------------------------------------------------

    public static void addPosition(BlockPos pos, List<BlockPos> list) {
        list.add(pos);
    }

    private static void writePositions(CompoundTag tag, String prefix, List<BlockPos> list) {
        tag.putInt("sexmod:pos_amount" + prefix, list.size());
        for (int i = 0; i < list.size(); i++) {
            BlockPos p = list.get(i);
            tag.putInt("sexmod:x" + prefix + i, p.getX());
            tag.putInt("sexmod:y" + prefix + i, p.getY());
            tag.putInt("sexmod:z" + prefix + i, p.getZ());
        }
    }

    private static void readPositions(CompoundTag tag, String prefix, List<BlockPos> list) {
        list.clear();
        int count = tag.getInt("sexmod:pos_amount" + prefix);
        for (int i = 0; i < count; i++) {
            int x = tag.getInt("sexmod:x" + prefix + i);
            int y = tag.getInt("sexmod:y" + prefix + i);
            int z = tag.getInt("sexmod:z" + prefix + i);
            list.add(new BlockPos(x, y, z));
        }
    }

    // -- Forge events ---------------------------------------------------------

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        level.getDataStorage().computeIfAbsent(
                GalathSpawnListData::load,
                GalathSpawnListData::new,
                DATA_NAME).setDirty();
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        level.getDataStorage().computeIfAbsent(
                GalathSpawnListData::load,
                GalathSpawnListData::new,
                DATA_NAME);
    }
}
