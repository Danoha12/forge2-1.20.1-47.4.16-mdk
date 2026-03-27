package com.trolmastercard.sexmod.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Random;

/**
 * SexmodStructure - port of the {@code d7} interface (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Marker interface for all NBT-based structure generators in the mod.
 * Replaces the 1.12.2 {@code d7} interface that extended no base class.
 *
 * Implementors: {@link StructurePlacer}
 */
public interface SexmodStructure {

    /**
     * Generate the structure at the given position.
     *
     * @return true if placement succeeded
     */
    boolean generate(Level level, Random random, BlockPos pos);
}
