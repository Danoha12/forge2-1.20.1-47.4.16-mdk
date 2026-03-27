package com.trolmastercard.sexmod.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * SexmodStructureConstants (d7) - Interface providing lazily-accessed
 * server-side constants for structure placement.
 *
 * In 1.12.2 these were static fields initialised at class-load time from
 * FMLCommonHandler; that caused NPEs before the server was ready.
 * In 1.20.1 we use lazy accessor methods instead.
 */
public interface SexmodStructureConstants {

    /** The overworld ServerLevel. Only call on the server thread. */
    static ServerLevel getOverworld() {
        return ServerLifecycleHooks.getCurrentServer().overworld();
    }

    /**
     * Default StructurePlaceSettings: no entities, no mirror, no rotation.
     * A new instance is returned each call (it is mutable).
     */
    static StructurePlaceSettings getPlacementSettings() {
        return new StructurePlaceSettings()
            .setIgnoreEntities(false)
            .setMirror(Mirror.NONE)
            .setRotation(Rotation.NONE);
    }
}
