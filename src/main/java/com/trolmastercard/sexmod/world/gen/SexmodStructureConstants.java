package com.trolmastercard.sexmod.world.gen; // Ajusta a tu paquete de generación

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * SexmodStructureConstants — Portado a 1.20.1.
 * * Utilidad estática para obtener referencias seguras al servidor y
 * * configuraciones de colocación de estructuras NBT.
 */
public final class SexmodStructureConstants {

    // 🛡️ Constructor privado: Evita que alguien instancie esta clase utilitaria
    private SexmodStructureConstants() {}

    /**
     * Obtiene el ServerLevel del Overworld.
     * 🚨 ADVERTENCIA: Solo debe llamarse en el hilo del servidor y DESPUÉS de que el mundo cargue.
     */
    public static ServerLevel getOverworld() {
        return ServerLifecycleHooks.getCurrentServer().overworld();
    }

    /**
     * Genera una nueva configuración de colocación por defecto:
     * sin entidades, sin espejo, sin rotación.
     */
    public static StructurePlaceSettings getPlacementSettings() {
        return new StructurePlaceSettings()
                .setIgnoreEntities(false)
                .setMirror(Mirror.NONE)
                .setRotation(Rotation.NONE);
    }
}