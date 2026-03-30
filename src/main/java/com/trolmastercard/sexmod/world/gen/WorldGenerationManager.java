package com.trolmastercard.sexmod.world.gen; // Ajusta a tu paquete de generación

import com.trolmastercard.sexmod.tribe.TribeManager; // Asegúrate de la importación
import com.trolmastercard.sexmod.util.NpcWorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * WorldGenerationManager — Portado a 1.20.1.
 * * Gestiona la generación en el mundo de estructuras NPC (refugios) y tribus.
 * * Usa SavedData para persistir qué chunks ya tienen estructuras y evitar duplicados.
 */
@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WorldGenerationManager extends SavedData {

    private static final String DATA_NAME = "sexmod:generation";
    private static final double TRIBE_SPAWN_CHANCE = 0.004D;

    private static WorldGenerationManager instance = null;
    private static boolean generationLock = true;

    private final List<GenerationRecord> records = new ArrayList<>();
    private final List<StructureEntry> entries = new ArrayList<>();

    // 🚨 1.20.1: Requerido para crear o cargar SavedData
    public static final SavedData.Factory<WorldGenerationManager> FACTORY = new SavedData.Factory<>(
            WorldGenerationManager::new,
            WorldGenerationManager::load,
            null
    );

    // ── Singleton ────────────────────────────────────────────────────────────

    public static WorldGenerationManager get(ServerLevel level) {
        if (instance == null) {
            // Se asegura de obtener los datos de la dimensión global (Overworld)
            instance = level.getServer().overworld().getDataStorage()
                    .computeIfAbsent(FACTORY, DATA_NAME);
        }
        return instance;
    }

    public WorldGenerationManager() {
        super();
        // Registro de estructuras por grupos de biomas (Tags de Minecraft 1.20.1)
        entries.add(new StructureEntry("ellie", biomeSet("is_forest", "is_taiga"), 9, true));
        entries.add(new StructureEntry("jenny", biomeSet("is_jungle"), 1, true));
        entries.add(new StructureEntry("bia", biomeSet("is_savanna", "is_badlands"), 2, true)); // is_mesa es ahora is_badlands
        entries.add(new StructureEntry("luna", biomeSet("is_ocean"), 0, false));
    }

    private static Set<TagKey<Biome>> biomeSet(String... tagNames) {
        Set<TagKey<Biome>> set = new HashSet<>();
        for (String name : tagNames) {
            // 🚨 1.20.1: Creación correcta de TagKeys para biomas
            set.add(TagKey.create(Registries.BIOME, new ResourceLocation("minecraft", name)));
        }
        return set;
    }

    // ── Ciclo de Vida SavedData ──────────────────────────────────────────────

    public static WorldGenerationManager load(CompoundTag tag) {
        WorldGenerationManager mgr = new WorldGenerationManager();
        CompoundTag inner = tag.getCompound(DATA_NAME);
        for (int i = 0; ; i++) {
            String name = inner.getString("sexmod:name" + i);
            String pos = inner.getString("sexmod:pos" + i);
            if (name.isEmpty() || pos.isEmpty()) break;
            mgr.records.add(new GenerationRecord(decodeChunkPos(pos), name));
        }
        return mgr;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag inner = new CompoundTag();
        int i = 0;
        for (GenerationRecord r : records) {
            inner.putString("sexmod:name" + i, r.structureName);
            inner.putString("sexmod:pos" + i, encodeChunkPos(r.chunkPos));
            i++;
        }
        tag.put(DATA_NAME, inner);
        return tag;
    }

    // ── Punto de Entrada de Generación ───────────────────────────────────────

    public void generate(ServerLevel level, RandomSource random, int chunkX, int chunkZ) {
        trySpawnTribe(level, random, chunkX, chunkZ);
        tryPlaceStructures(level, chunkX, chunkZ);
        trySpawnGoblinCamp(level, random, chunkX, chunkZ);
    }

    // ── Tribus ───────────────────────────────────────────────────────────────

    void trySpawnTribe(ServerLevel level, RandomSource random, int cx, int cz) {
        if (random.nextDouble() > TRIBE_SPAWN_CHANCE) return;

        int x = cx * 16 + 8;
        int z = cz * 16 + 8;
        // Asumiendo que tu utilidad NpcWorldUtil existe y ha sido portada
        int y = NpcWorldUtil.getSurfaceY(level, x, z);

        BlockPos pos = new BlockPos(x, y, z);
        if (level.getBlockState(pos).isSolid() && level.getBlockState(pos).is(Blocks.WATER)) return;

        TribeManager.spawnTribeAt(level, new Vec3(x, y, z));
    }

    // ── Ubicación de Estructuras ─────────────────────────────────────────────

    void tryPlaceStructures(ServerLevel level, int cx, int cz) {
        if (!generationLock) return;
        generationLock = false;
        for (StructureEntry e : entries) tryPlaceEntry(e, level, cx, cz);
        generationLock = true;
    }

    void tryPlaceEntry(StructureEntry entry, ServerLevel level, int cx, int cz) {
        // Evitar duplicados por distancia
        for (GenerationRecord r : records) {
            if (r.structureName.equals(entry.name)) {
                ChunkPos cp = r.chunkPos;
                if (Math.abs(cp.x - cx) < entry.minDistChunks || Math.abs(cp.z - cz) < entry.minDistChunks) return;
            }
        }

        int x = cx * 16 + 8;
        int z = cz * 16 + 8;

        Holder<Biome> biome = level.getBiome(new BlockPos(x, 80, z));

        // Comprobar si el bioma coincide con alguna de las etiquetas permitidas
        boolean inBiome = entry.biomeTags.stream().anyMatch(biome::is);
        if (!inBiome) return;

        // Comprobación de altura y terreno seco
        int maxY = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;
        int sx = 9, sz = 9;

        for (int i = x; i < x + sx; i++) {
            for (int k = z; k < z + sz; k++) {
                int sy = NpcWorldUtil.getSurfaceY(level, i, k);
                if (entry.requiresDryGround && level.getBlockState(new BlockPos(i, sy, k)).is(Blocks.WATER)) return;
                if (sy > maxY) maxY = sy;
                if (sy < minY) minY = sy;
            }
        }

        if (maxY - minY > entry.maxHeightDiff) return;

        // Marcar como generado y colocar
        records.add(new GenerationRecord(new ChunkPos(cx, cz), entry.name));

        // 🚨 Llamada actualizada a tu StructurePlacer corregido
        new StructurePlacer(entry.name).place(level, new BlockPos(x, maxY, z));
        this.setDirty(); // Marcar datos para ser guardados
    }

    // ── Campamentos Goblin ───────────────────────────────────────────────────

    void trySpawnGoblinCamp(ServerLevel level, RandomSource random, int cx, int cz) {
        if (random.nextDouble() > 0.005D) return;
        int x = cx * 16 + 3;
        int z = cz * 16 + 3;
        int y = random.nextInt(255);
        BlockPos pos = new BlockPos(x, y, z);

        new StructurePlacer("goblin").place(level, pos);
    }

    // ── Eventos Forge (Automáticos) ──────────────────────────────────────────

    @SubscribeEvent
    public static void onWorldSave(LevelEvent.Save event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            get(level).setDirty();
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            instance = get(level);
        }
    }

    // ── Utilidades Codec ─────────────────────────────────────────────────────

    static String encodeChunkPos(ChunkPos pos) {
        return pos.x + "|" + pos.z;
    }

    static ChunkPos decodeChunkPos(String s) {
        String[] p = s.split("\\|");
        return new ChunkPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    // ── Clases Internas ──────────────────────────────────────────────────────

    static class GenerationRecord {
        ChunkPos chunkPos;
        String structureName;

        GenerationRecord(ChunkPos pos, String name) {
            this.chunkPos = pos;
            this.structureName = name;
        }
    }

    static class StructureEntry {
        String name;
        Set<TagKey<Biome>> biomeTags;
        int minDistChunks;
        boolean requiresDryGround;
        int maxHeightDiff = 9;

        StructureEntry(String name, Set<TagKey<Biome>> biomeTags, int minDistChunks, boolean requiresDryGround) {
            this.name = name;
            this.biomeTags = biomeTags;
            this.minDistChunks = minDistChunks;
            this.requiresDryGround = requiresDryGround;
        }
    }
}