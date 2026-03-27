package com.trolmastercard.sexmod.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * ModLootTables - ported from dz.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Registers and holds {@link ResourceLocation}s for the mod's custom loot tables.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - LootTableList.func_186375_a - BuiltInLootTables.register (returns the same RL)
 *     In 1.20.1 loot tables are discovered automatically from data packs;
 *     BuiltInLootTables.register() just records the location in the built-in set.
 */
public class ModLootTables {

    public static final ResourceLocation JENNY = register("jenny");
    public static final ResourceLocation ELLIE = register("ellie");
    public static final ResourceLocation SLIME  = register("slime");
    public static final ResourceLocation BIA    = register("bia");

    private static ResourceLocation register(String name) {
        return BuiltInLootTables.register(new ResourceLocation("sexmod", name));
    }
}
