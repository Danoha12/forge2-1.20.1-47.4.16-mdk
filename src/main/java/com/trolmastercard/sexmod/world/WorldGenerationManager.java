package com.trolmastercard.sexmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * Manages world-generation of NPC structures (shelters) and tribe summoning.
 * Persists generated chunk positions via SavedData so structures are not duplicated.
 *
 * Equivalent to 1.12.2 {@code g3} which implemented {@code IWorldGenerator + WorldSavedData}.
 * In 1.20.1 actual chunk generation uses {@link net.minecraftforge.event.level.ChunkEvent}.
 *
 * Obfuscated name: g3
 */
public class WorldGenerationManager extends SavedData {

    private static final String DATA_NAME = "sexmod:generation";
    private static final double TRIBE_SPAWN_CHANCE = 0.004D;

    private static WorldGenerationManager instance = null;
    private static boolean generationLock = true;

    private final List<GenerationRecord> records = new ArrayList<>();
    private final List<StructureEntry>   entries  = new ArrayList<>();

    // -- Singleton -------------------------------------------------------------

    public static WorldGenerationManager get() {
        if (instance == null) instance = new WorldGenerationManager();
        return instance;
    }

    public WorldGenerationManager() {
        super();
        instance = this;
        // Register structure entries per biome group
        entries.add(new StructureEntry("ellie",  biomeSet("is_forest", "old_growth_taiga"), 9, true));
        entries.add(new StructureEntry("jenny",  biomeSet("is_jungle"),                      1, true));
        entries.add(new StructureEntry("bia",    biomeSet("is_savanna", "is_mesa"),          2, true));
        entries.add(new StructureEntry("luna",   biomeSet("is_ocean"),                       0, false));
    }

    private static Set<String> biomeSet(String... tags) {
        return new HashSet<>(Arrays.asList(tags));
    }

    // -- SavedData lifecycle ---------------------------------------------------

    public static WorldGenerationManager load(CompoundTag tag) {
        WorldGenerationManager mgr = new WorldGenerationManager();
        CompoundTag inner = tag.getCompound(DATA_NAME);
        for (int i = 0; ; i++) {
            String name = inner.getString("sexmod:name" + i);
            String pos  = inner.getString("sexmod:pos" + i);
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
            inner.putString("sexmod:pos"  + i, encodeChunkPos(r.chunkPos));
            i++;
        }
        tag.put(DATA_NAME, inner);
        return tag;
    }

    // -- Generation entry point (called from ChunkEvent.Load or similar) --------

    public void generate(ServerLevel level, Random random, int chunkX, int chunkZ) {
        trySpawnTribe(level, random, chunkX, chunkZ);
        tryPlaceStructures(level, random, chunkX, chunkZ);
        trySpawnGoblinCamp(level, random, chunkX, chunkZ);
    }

    // -- Tribe spawn -----------------------------------------------------------

    void trySpawnTribe(ServerLevel level, Random random, int cx, int cz) {
        if (random.nextDouble() > TRIBE_SPAWN_CHANCE) return;
        int x = cx * 16 + 8;
        int z = cz * 16 + 8;
        int y = NpcWorldUtil.getSurfaceY(level, x, z);
        if (level.getBlockState(new BlockPos(x, y, z)).isSolid() &&
                level.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.WATER) return;
        TribeManager.spawnTribeAt(level, new Vec3(x, y, z));
    }

    // -- Structure placement ---------------------------------------------------

    void tryPlaceStructures(ServerLevel level, Random random, int cx, int cz) {
        if (!generationLock) return;
        generationLock = false;
        for (StructureEntry e : entries) tryPlaceEntry(e, level, random, cx, cz);
        generationLock = true;
    }

    void tryPlaceEntry(StructureEntry entry, ServerLevel level, Random random, int cx, int cz) {
        for (GenerationRecord r : records) {
            if (r.structureName.equals(entry.name)) {
                ChunkPos cp = r.chunkPos;
                if (Math.abs(cp.x - cx) < entry.minDistChunks ||
                    Math.abs(cp.z - cz) < entry.minDistChunks) return;
            }
        }

        int x = cx * 16 + 8;
        int z = cz * 16 + 8;

        Holder<Biome> biome = level.getBiome(new BlockPos(x, 80, z));
        boolean inBiome = entry.biomeTags.stream().anyMatch(tag ->
                biome.is(new ResourceLocation("minecraft", tag)));
        if (!inBiome) return;

        // Height check
        int maxY = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;
        int sx = 9, sz = 9; // approximate structure footprint
        for (int i = x; i < x + sx; i++) {
            for (int k = z; k < z + sz; k++) {
                int sy = NpcWorldUtil.getSurfaceY(level, i, k);
                if (entry.requiresDryGround &&
                        level.getBlockState(new BlockPos(i, sy, k)).getBlock() == Blocks.WATER) return;
                if (sy > maxY) maxY = sy;
                if (sy < minY) minY = sy;
            }
        }
        if (maxY - minY > entry.maxHeightDiff) return;

        records.add(new GenerationRecord(new ChunkPos(cx, cz), entry.name));
        new StructurePlacer(entry.name).place(level, random, new BlockPos(x, maxY, z));
        setDirty();
    }

    // -- Goblin camp generation ------------------------------------------------

    void trySpawnGoblinCamp(ServerLevel level, Random random, int cx, int cz) {
        // Goblin camps placed very rarely in plains biomes
        if (random.nextDouble() > 0.005D) return;
        int x = cx * 16 + 3;
        int z = cz * 16 + 3;
        int y = random.nextInt(255);
        BlockPos pos = new BlockPos(x, y, z);
        // Delegate actual structure placement + entity spawn to StructurePlacer
        new StructurePlacer("goblin").place(level, random, pos);
    }

    // -- Forge events ---------------------------------------------------------

    @SubscribeEvent
    public void onWorldSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        level.getDataStorage().computeIfAbsent(
                WorldGenerationManager::load,
                WorldGenerationManager::new,
                DATA_NAME).setDirty();
    }

    @SubscribeEvent
    public void onWorldLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        instance = level.getDataStorage().computeIfAbsent(
                WorldGenerationManager::load,
                WorldGenerationManager::new,
                DATA_NAME);
    }

    // -- Codec helpers ---------------------------------------------------------

    static String encodeChunkPos(ChunkPos pos) {
        return pos.x + "|" + pos.z;
    }

    static ChunkPos decodeChunkPos(String s) {
        String[] p = s.split("\\|");
        return new ChunkPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    // -- Inner data classes ----------------------------------------------------

    static class GenerationRecord {
        ChunkPos chunkPos;
        String   structureName;
        GenerationRecord(ChunkPos pos, String name) {
            this.chunkPos      = pos;
            this.structureName = name;
        }
    }

    static class StructureEntry {
        String       name;
        Set<String>  biomeTags;
        int          minDistChunks;
        boolean      requiresDryGround;
        int          maxHeightDiff = 9;

        StructureEntry(String name, Set<String> biomeTags, int minDistChunks, boolean requiresDryGround) {
            this.name              = name;
            this.biomeTags         = biomeTags;
            this.minDistChunks     = minDistChunks;
            this.requiresDryGround = requiresDryGround;
        }
    }
}
