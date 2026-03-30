package com.trolmastercard.sexmod.registry; // Te recomiendo moverlo a un paquete de registros

import net.minecraft.resources.ResourceLocation;

/**
 * ModLootTables — Portado a 1.20.1.
 * * En 1.20.1 ya no se usa BuiltInLootTables.register() para los mods.
 * * Las tablas se cargan automáticamente desde data/sexmod/loot_tables/.
 * * Solo necesitamos mantener las referencias estáticas para usarlas en el código.
 */
public class ModLootTables {

    public static final ResourceLocation JENNY = new ResourceLocation("sexmod", "jenny");
    public static final ResourceLocation ELLIE = new ResourceLocation("sexmod", "ellie");
    public static final ResourceLocation SLIME = new ResourceLocation("sexmod", "slime");
    public static final ResourceLocation BIA   = new ResourceLocation("sexmod", "bia");

}