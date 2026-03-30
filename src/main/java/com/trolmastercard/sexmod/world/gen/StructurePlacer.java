package com.trolmastercard.sexmod.world.gen; // Ajusta a tu paquete de generación

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.Optional;

/**
 * StructurePlacer — Portado a 1.20.1.
 * * Coloca plantillas NBT desde la carpeta data/sexmod/structures/.
 */
public class StructurePlacer implements SexmodStructure {

    public final String templatePath;

    public StructurePlacer(String templatePath) {
        this.templatePath = templatePath;
    }

    // ── Colocación sin rotación ──────────────────────────────────────────────

    public boolean place(Level level, BlockPos pos) {
        return placeRotated(level, pos, Rotation.NONE);
    }

    // ── Colocación con rotación ──────────────────────────────────────────────

    public boolean placeRotated(Level level, BlockPos pos, Rotation rotation) {
        // CRÍTICO: Las estructuras solo pueden generarse en el lado del servidor
        if (!(level instanceof ServerLevel serverLevel)) return false;

        StructureTemplateManager mgr = serverLevel.getServer().getStructureManager();
        ResourceLocation rl = new ResourceLocation("sexmod", templatePath);

        Optional<StructureTemplate> templateOpt = mgr.get(rl);
        if (templateOpt.isEmpty()) {
            System.out.println("[SexMod] Error: No se encontró la estructura NBT en: " + rl);
            return false;
        }

        StructureTemplate template = templateOpt.get();

        // 🛡️ 1.20.1: Usamos la utilidad que tú mismo creaste para evitar configuraciones mutadas
        StructurePlaceSettings settings = SexmodStructureConstants.getPlacementSettings()
                .setRotation(rotation);

        // Flag 2 (BLOCK_UPDATE) es el estándar seguro para rotaciones
        // placeInWorld devuelve un booleano si la colocación fue exitosa
        return template.placeInWorld(serverLevel, pos, pos, settings, serverLevel.getRandom(), 2);
    }

    // ── SexmodStructure Contract ─────────────────────────────────────────────

    // 🚨 CORREGIDO: Usando RandomSource en lugar de java.util.Random
    @Override
    public boolean generate(Level level, RandomSource random, BlockPos pos) {
        return place(level, pos);
    }
}