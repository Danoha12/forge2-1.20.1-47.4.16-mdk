package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * InteractionMeterOverlay — Portado a 1.20.1.
 * * Renderiza una barra de progreso cuando el jugador mira a un NPC.
 * * Utiliza el sistema de EntityData de BaseNpcEntity para obtener el valor en tiempo real.
 */
public class InteractionMeterOverlay {

    // 🎨 Asegúrate de que esta textura exista en: src/main/resources/assets/sexmod/textures/gui/interaction_meter.png
    private static final ResourceLocation METER_TEXTURE = new ResourceLocation("sexmod", "textures/gui/interaction_meter.png");

    public static final IGuiOverlay HUD_METER = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();

        // 🛡️ Validaciones básicas de seguridad
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        // 🔍 SENSOR: ¿El raytrace del jugador está chocando con una entidad?
        if (mc.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof BaseNpcEntity npc) {

            // 📊 Obtener el nivel de interacción sincronizado (0.0 a 1.0)
            float progress = npc.getInteractionLevel();

            // Posicionamiento: Centrado horizontalmente, cerca de la hotbar
            int x = width / 2 - 91;
            int y = height - 55; // Ajustado un poco más arriba de la hotbar para que no choque

            RenderSystem.setShaderTexture(0, METER_TEXTURE);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            // 1. Dibujar el fondo de la barra (Supongamos que en el PNG está en V=0)
            // blit(texture, x, y, u, v, width, height)
            guiGraphics.blit(METER_TEXTURE, x, y, 0, 0, 182, 5);

            // 2. Dibujar el relleno (Supongamos que en el PNG está justo debajo, en V=5)
            int fillerWidth = (int) (progress * 182);
            if (fillerWidth > 0) {
                guiGraphics.blit(METER_TEXTURE, x, y, 0, 5, fillerWidth, 5);
            }

            // 3. Dibujar el nombre del NPC (Opcional, pero queda genial)
            // Usamos un color blanco (0xFFFFFF) con una pequeña sombra
            String npcName = npc.getDisplayName().getString();
            guiGraphics.drawCenteredString(mc.font, npcName, width / 2, y - 12, 0xFFFFFF);

            RenderSystem.disableBlend();
        }
    };
}