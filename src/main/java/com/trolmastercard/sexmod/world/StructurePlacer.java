package com.trolmastercard.sexmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/**
 * StructurePlacer - ported from b4.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Places NBT structure templates from the mod's {@code structures/} resource folder
 * at a given world position.  Implements the {@link SexmodStructure} interface so it
 * can be registered and referenced generically by the world-gen system.
 *
 * In 1.12.2:
 *   - Extended {@code WorldGenerator} and implemented interface {@code d7}.
 *   - Used {@code TemplateManager} obtained from {@code b.func_184163_y()} (the server).
 *   - Called {@code template.func_186253_b(world, pos, placementSettings)}.
 *   - {@code Template.PlacementData.a} was a static default instance.
 *
 * In 1.20.1:
 *   - Standalone class (no WorldGenerator equivalent for direct NBT placement).
 *   - {@code TemplateManager} - {@link StructureTemplateManager}.
 *   - Obtained via {@code level.getServer().getStructureManager()}.
 *   - {@code template.placeInWorld(level, pos, pos, settings, rand, flags)}.
 *   - Default placement settings = no rotation, no mirror, with update flags.
 *
 * Usage:
 * <pre>
 *   StructurePlacer placer = new StructurePlacer("tribe/camp_small");
 *   placer.place(level, pos);
 *   placer.placeRotated(level, pos, Rotation.CLOCKWISE_90);
 * </pre>
 */
public class StructurePlacer implements SexmodStructure {

    /** Path relative to {@code data/sexmod/structures/}, without extension. */
    public final String templatePath;

    private static final StructurePlaceSettings DEFAULT_SETTINGS =
        new StructurePlaceSettings()
            .setMirror(Mirror.NONE)
            .setRotation(Rotation.NONE)
            .setIgnoreEntities(false);

    public StructurePlacer(String templatePath) {
        this.templatePath = templatePath;
    }

    // =========================================================================
    //  Place without rotation
    //  Original: b4.a(World, BlockPos)
    // =========================================================================

    /**
     * Places the structure template at {@code pos} with default (no) rotation.
     */
    public void place(Level level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null) return;

        StructureTemplateManager mgr = server.getStructureManager();
        ResourceLocation rl = new ResourceLocation("sexmod", templatePath);
        StructureTemplate template = mgr.getOrCreate(rl);

        level.getBlockState(pos);  // prime the chunk
        template.placeInWorld(level, pos, pos, DEFAULT_SETTINGS,
            level.getRandom(), 3);
    }

    // =========================================================================
    //  Place with rotation
    //  Original: b4.a(World, BlockPos, Rotation)
    // =========================================================================

    /**
     * Places the structure template at {@code pos} with the given {@link Rotation}.
     *
     * Original: {@code b4.a(World, BlockPos, Rotation)} used update flags = 2.
     * In 1.20.1 we use {@code 2} (BLOCK_UPDATE) so the result behaves identically.
     */
    public void placeRotated(Level level, BlockPos pos, Rotation rotation) {
        MinecraftServer server = level.getServer();
        if (server == null) return;

        StructureTemplateManager mgr = server.getStructureManager();
        ResourceLocation rl = new ResourceLocation("sexmod", templatePath);
        StructureTemplate template = mgr.getOrCreate(rl);

        StructurePlaceSettings settings = new StructurePlaceSettings()
            .setMirror(Mirror.NONE)
            .setRotation(rotation)
            .setIgnoreEntities(false);

        level.getBlockState(pos);  // prime the chunk
        template.placeInWorld(level, pos, pos, settings, level.getRandom(), 2);
    }

    // =========================================================================
    //  SexmodStructure
    // =========================================================================

    @Override
    public boolean generate(Level level, java.util.Random random, BlockPos pos) {
        place(level, pos);
        return true;
    }
}
