package com.trolmastercard.sexmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/**
 * NpcLootTables - ported from dz.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Registers and stores loot table {@link ResourceLocation}s for each NPC type.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - LootTableList.func_186375_a(rl) - BuiltInLootTables.register(rl)
 *   - new ResourceLocation("sexmod", name) - same API
 */
public class NpcLootTables {

    /** Jenny's loot table. */
    public static final ResourceLocation JENNY =
            BuiltInLootTables.register(new ResourceLocation("sexmod", "jenny"));

    /** Ellie's loot table. */
    public static final ResourceLocation ELLIE =
            BuiltInLootTables.register(new ResourceLocation("sexmod", "ellie"));

    /** Slime's loot table. */
    public static final ResourceLocation SLIME =
            BuiltInLootTables.register(new ResourceLocation("sexmod", "slime"));

    /** Bia's loot table. */
    public static final ResourceLocation BIA =
            BuiltInLootTables.register(new ResourceLocation("sexmod", "bia"));
}
