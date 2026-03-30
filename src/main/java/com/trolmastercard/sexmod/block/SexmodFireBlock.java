package com.trolmastercard.sexmod.block; // Ajusta a tu paquete de bloques

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * SexmodFireBlock — Portado a 1.20.1.
 * * Bloque de fuego decorativo que no se propaga ni se apaga.
 * * Ideal para braseros y estructuras del mod.
 */
public class SexmodFireBlock extends BaseFireBlock {

    /**
     * @param properties Propiedades base del bloque
     * @param fireDamage Cuánto daño hace el fuego al pisarlo (1.0f = daño normal)
     */
    public SexmodFireBlock(BlockBehaviour.Properties properties, float fireDamage) {
        super(properties, fireDamage);
    }

    // ── Propiedades Base (Para tu DeferredRegister) ──────────────────────────

    /**
     * 🚨 1.20.1: Usamos un método para generar las propiedades de forma segura
     * durante la fase de registro, evitando crasheos de inicialización estática.
     */
    public static BlockBehaviour.Properties createProperties() {
        return BlockBehaviour.Properties.of()
                .noCollission()      // Atravesable
                .instabreak()        // Se rompe de un golpe
                .lightLevel(s -> 15) // Brillo máximo
                .sound(SoundType.WOOL) // Fuego silencioso al romper
                .noOcclusion()       // No bloquea la vista
                .noLootTable();      // No suelta ítems al romperse
    }

    // ── Comportamiento de Fuego Eterno ───────────────────────────────────────

    /**
     * Sobrescribimos el tick aleatorio para evitar que el fuego se propague,
     * se apague por la lluvia, o interactúe con el entorno. Se queda ahí para siempre.
     */
    @Override
    public void randomTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Intencionalmente vacío — Fuego eterno y seguro.
    }
}