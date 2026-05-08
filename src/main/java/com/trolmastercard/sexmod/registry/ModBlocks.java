package com.trolmastercard.sexmod.registry; // 🚨 Ruta corregida para que el Main lo encuentre

import com.trolmastercard.sexmod.util.ModConstants; // Importamos tu ID constante
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/** * ModBlocks - Registro de bloques (Diseño híbrido ajustado para tu chasis)
 */
public class ModBlocks {

    // El motor de bloques usando la excelente sugerencia de Claude
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ModConstants.MOD_ID);

    // 🚨 El cable de encendido que le faltaba al otro maistro
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}