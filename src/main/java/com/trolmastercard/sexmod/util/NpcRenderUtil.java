package com.trolmastercard.sexmod.util; // Ajusta al paquete correcto

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * NpcRenderUtil — Portado a 1.20.1.
 * * Utilidades para el suavizado de posición (LERP) y control de iluminación.
 * * Optimizada usando los métodos de interpolación nativos de Mojang.
 */
@OnlyIn(Dist.CLIENT)
public final class NpcRenderUtil {

    private NpcRenderUtil() {}

    // ── Diferencia de posición de ojos con el jugador ────────────────────────

    /**
     * Calcula la distancia entre los ojos del NPC y la cámara del jugador,
     * usando los métodos nativos de 1.20.1 para evitar tirones visuales.
     */
    public static Vec3 getOffsetFromPlayer(Entity entity, Player player, float partialTick) {
        // En 1.20.1, getEyePosition ya hace el LERP internamente. ¡Magia pura!
        Vec3 npcEye = entity.getEyePosition(partialTick);

        // Asumo que querías la posición base del jugador (pies) para la resta original,
        // pero si también querías los ojos, cambiarías getPosition() por getEyePosition()
        Vec3 plPos = player.getPosition(partialTick);

        return npcEye.subtract(plPos);
    }

    // ── Diferencia de posición base entre dos entidades ──────────────────────

    public static Vec3 getEntityOffset(Entity entity, Player player, float partialTick) {
        Vec3 entityPos = getLerpedPos(entity, partialTick);
        if (player == null) return entityPos;

        Vec3 playerPos = player.getPosition(partialTick); // LERP nativo
        return entityPos.subtract(playerPos);
    }

    // ── Corazón del suavizado (Soporte para SmoothPos custom) ────────────────

    /**
     * Devuelve la posición interpolada de la entidad.
     * Si es un NPC en escena con cinemática, usa su "SmoothPos" personalizado.
     */
    public static Vec3 getLerpedPos(Entity entity, float partialTick) {
        if (entity instanceof BaseNpcEntity npc && npc.hasSmoothPos()) {
            return npc.getSmoothPos(); // El LERP manual de tu cinemática
        }

        // Si no es un NPC en cinemática, usamos el LERP nativo ultra rápido
        return entity.getPosition(partialTick);
    }

    // ── Iluminación y Shaders ────────────────────────────────────────────────

    /**
     * Fuerza el color del shader al máximo (blanco puro sin sombras).
     * Ojo: En 1.20.1 esto afecta al GameRenderer global, úsalo con cuidado
     * y recuerda restaurarlo.
     */
    public static void setFullBright() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Helper para obtener el valor empaquetado de luz máxima (FULL_BRIGHT = 15728880) */
    public static int getFullLight() {
        return LightTexture.FULL_BRIGHT; // (240 << 16 | 240)
    }
}