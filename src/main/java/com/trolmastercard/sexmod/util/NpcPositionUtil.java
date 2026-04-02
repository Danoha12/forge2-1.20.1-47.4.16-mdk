package com.trolmastercard.sexmod.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * NpcPositionUtil — Utilidad matemática para calcular posiciones relativas.
 * * Portado a 1.20.1.
 */
public class NpcPositionUtil {

    /**
     * Calcula el vector de diferencia (offset) entre el NPC y el Jugador.
     * Utiliza la interpolación de ticks parciales para que el seguimiento
     * de la cabeza/brazos sea suave y no tiemble al moverse.
     *
     * @param npc Entidad base (ej. Galath)
     * @param player El jugador objetivo
     * @param partialTick El frame-time actual de Minecraft
     * @return El vector 3D que apunta del NPC al jugador.
     */
    public static Vec3 getOffsetToPlayer(Entity npc, Entity player, float partialTick) {
        if (npc == null || player == null) {
            return Vec3.ZERO;
        }

        // getPosition() en 1.20.1 ya interpola automáticamente usando el partialTick
        Vec3 playerPos = player.getPosition(partialTick);

        // Elevamos ligeramente el objetivo para que mire a la cabeza/pecho del jugador
        // en lugar de mirarle los pies (el y+1.5 suele ser la altura de los ojos)
        playerPos = playerPos.add(0, 1.5, 0);

        Vec3 npcPos = npc.getPosition(partialTick);

        return playerPos.subtract(npcPos);
    }
}