package com.trolmastercard.sexmod.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * GalathSpawnListData — Portado a 1.20.1.
 * * Persiste las posiciones de spawn de Galath y MangleLie entre sesiones.
 * * Optimizado para usar arreglos de Longs (BlockPos.asLong) en lugar de entradas individuales.
 */
public class GalathSpawnListData extends SavedData {

    private static final String DATA_NAME = "sexmod_spawn_data";

    // Nota: Eliminamos el 'static' para que cada dimensión sea independiente.
    private final List<BlockPos> galathPositions = new ArrayList<>();
    private final List<BlockPos> mangPositions = new ArrayList<>();

    public GalathSpawnListData() {}

    // ── Lógica de Carga y Guardado (NBT) ─────────────────────────────────────

    public static GalathSpawnListData load(CompoundTag tag) {
        GalathSpawnListData data = new GalathSpawnListData();

        // Cargamos posiciones de Galath
        long[] gPosArray = tag.getLongArray("GalathPositions");
        for (long l : gPosArray) {
            data.galathPositions.add(BlockPos.of(l));
        }

        // Cargamos posiciones de MangleLie
        long[] mPosArray = tag.getLongArray("MangPositions");
        for (long l : mPosArray) {
            data.mangPositions.add(BlockPos.of(l));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Guardamos Galath como LongArray (Mucho más eficiente)
        List<Long> gLongs = galathPositions.stream().map(BlockPos::asLong).toList();
        tag.putLongArray("GalathPositions", gLongs);

        // Guardamos MangleLie
        List<Long> mLongs = mangPositions.stream().map(BlockPos::asLong).toList();
        tag.putLongArray("MangPositions", mLongs);

        return tag;
    }

    // ── Getters y Helpers ────────────────────────────────────────────────────

    public List<BlockPos> getGalathPositions() { return galathPositions; }
    public List<BlockPos> getMangPositions() { return mangPositions; }

    public void addGalathPos(BlockPos pos) {
        galathPositions.add(pos);
        this.setDirty(); // VITAL: Avisa a Minecraft que debe guardar el archivo en disco
    }

    /** * Método estático para obtener los datos de una dimensión específica. */
    public static GalathSpawnListData get(ServerLevel level) {
        return level.getDataStorage().computeAbsent(
                GalathSpawnListData::load,
                GalathSpawnListData::new,
                DATA_NAME
        );
    }

    // ── Manejo de Eventos (Automático) ───────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onLevelSave(LevelEvent.Save event) {
            // No necesitamos forzar nada aquí, SavedData se guarda solo si llamamos a setDirty()
        }

        @SubscribeEvent
        public static void onLevelLoad(LevelEvent.Load event) {
            if (event.getLevel() instanceof ServerLevel level) {
                // Pre-cargamos los datos apenas se carga la dimensión
                GalathSpawnListData.get(level);
            }
        }
    }
}