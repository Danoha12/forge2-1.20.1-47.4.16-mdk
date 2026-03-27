package com.trolmastercard.sexmod.entity;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * GalathFlightController - Controlador HUD de Vuelo de Galath.
 * Portado a 1.20.1.
 * * Renderiza un overlay HUD mostrando hasta 3 íconos de "carga de vuelo"
 * * cuando el jugador está montando/interactuando con Galath en el aire.
 * * Cada carga se anima al consumirse o regenerarse.
 */
@OnlyIn(Dist.CLIENT)
public class GalathFlightController {

    // =========================================================================
    //  Texturas y Dimensiones
    // =========================================================================

    static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/gui/galath_flight_ui.png");

    // Rectángulos de Textura: (u, v, width, height)
    static final int[] RECT_BG        = { 0, 77, 128, 41 };
    static final int[] RECT_ICON_FULL = { 0,  0,  23, 36 };
    static final int[] RECT_ICON_EMPTY= { 0, 36,  23, 36 };
    static final int[] RECT_CONNECTOR = {23,  2,  20, 31 };

    // =========================================================================
    //  Constantes de Tiempo y Animación
    // =========================================================================

    static long  REGEN_DELAY_MS     = 3000L;
    static long  REGEN_COOLDOWN_MS  = 5000L;
    static final long FADE_DURATION = 500L;

    // Tablas de búsqueda de animación (índices 0-2)
    static final float[] ICON_OFFSET_X = { -14.25F, -15.5F, -16.875F };
    static final float[] ICON_Y_OFFSET = {  37.5F,   43.0F,  45.0F   };

    static final float TRANSLATE_Y   = -11.25F;
    static final float SCALE_ADD     =  0.075F;
    static final float SCALE_EXIT    = -0.15F;

    static final int   BOTTOM_OFFSET = 70;
    static final int   MAX_CHARGES   = 3;

    // =========================================================================
    //  Estado Estático (HUD)
    // =========================================================================

    static boolean visible     = false;
    static int     charges     = 3;

    static long lastUsedTime   = 0L;
    static long lastRegenTime  = 0L;
    static long fadeInStart    = 0L;
    static long fadeOutStart   = Long.MAX_VALUE - 500L;

    // =========================================================================
    //  API Pública
    // =========================================================================

    /** Retorna true si hay al menos una carga y el timer lo permite. */
    public static boolean canUseCharge() {
        if (charges <= 0) return false;
        return (System.currentTimeMillis() - lastUsedTime > REGEN_DELAY_MS);
    }

    /** Consume una carga. */
    public static void useCharge() {
        charges--;
        lastUsedTime = System.currentTimeMillis();
    }

    /** Muestra el overlay (Llamado al montar a Galath). */
    public static void show() {
        if (visible) return;
        visible      = true;
        fadeInStart  = System.currentTimeMillis();
        fadeOutStart = Long.MAX_VALUE - 500L;
    }

    /** Comienza el desvanecimiento. */
    public static void startFadeOut() {
        fadeOutStart = System.currentTimeMillis();
    }

    /** Oculta el HUD inmediatamente y reinicia variables. */
    public static void hide() {
        visible      = false;
        fadeOutStart = Long.MAX_VALUE - 500L;
        fadeInStart  = 0L;
    }

    // =========================================================================
    //  Lógica de Tick (Llamado en renderizado)
    // =========================================================================

    private static void tickRegen() {
        if (charges == MAX_CHARGES) return;
        long now = System.currentTimeMillis();
        if (now - Math.max(lastUsedTime, lastRegenTime) < REGEN_COOLDOWN_MS) return;
        charges++;
        lastRegenTime = now;
    }

    // =========================================================================
    //  Renderizado (IGuiOverlay para Forge 1.20.1)
    // =========================================================================

    // Deberás registrar esto en tu clase de Setup de Cliente:
    // OverlayRegistry.register(OVERLAY_ID, GalathFlightController.OVERLAY);
    public static final IGuiOverlay OVERLAY = (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        tickRegen();
        if (!visible) return;

        long now = System.currentTimeMillis();

        if (now - fadeOutStart > FADE_DURATION) {
            hide();
            return;
        }

        float alpha;
        if (now < fadeInStart + FADE_DURATION) {
            alpha = (float)(now - fadeInStart) / FADE_DURATION;
        } else if (now < fadeOutStart + FADE_DURATION) {
            alpha = 1.0F - (float)(now - fadeOutStart) / FADE_DURATION;
        } else {
            alpha = 1.0F;
        }
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        int cx = screenWidth / 2;
        int by = screenHeight - BOTTOM_OFFSET;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        // Fondo (Barra)
        blit(guiGraphics, cx - RECT_BG[2] / 2, by - RECT_BG[3], RECT_BG[0], RECT_BG[1], RECT_BG[2], RECT_BG[3]);

        // Conectores (Tubos entre cargas)
        blit(guiGraphics, (int)(cx - 1.5F * RECT_ICON_FULL[2] + 1), by - BOTTOM_OFFSET + 3,
                RECT_CONNECTOR[0], RECT_CONNECTOR[1], RECT_CONNECTOR[2], RECT_CONNECTOR[3]);
        blit(guiGraphics, cx - RECT_ICON_FULL[2] / 2 + 1, by - BOTTOM_OFFSET + 3,
                RECT_CONNECTOR[0], RECT_CONNECTOR[1], RECT_CONNECTOR[2], RECT_CONNECTOR[3]);
        blit(guiGraphics, cx + RECT_ICON_FULL[2] / 2 + 1, by - BOTTOM_OFFSET + 3,
                RECT_CONNECTOR[0], RECT_CONNECTOR[1], RECT_CONNECTOR[2], RECT_CONNECTOR[3]);

        // Progreso de vaciado (Animación matemática)
        float drainProgress = charges >= 0 && charges < MAX_CHARGES
                ? (float) Math.sin(Math.PI * Mth.clamp(1.0F - (float)(now - lastUsedTime) / 500.0F, 0, 1)) * 0.5F
                : 0.0F;

        // Dibuja los iconos individuales
        for (int i = 1; i <= MAX_CHARGES; i++) {
            renderChargeIcon(guiGraphics, i, charges, drainProgress, cx, by, alpha);
        }

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    };

    // =========================================================================
    //  Helpers de Renderizado
    // =========================================================================

    private static void renderChargeIcon(GuiGraphics guiGraphics, int slot, int current,
                                         float drainProgress, int cx, int by, float alpha) {

        float exitAmt  = (slot == current + 1) ? drainProgress : 0.0F;
        float enterAmt = 0.0F; // Uso futuro si se agrega animación de entrada

        float scale = 1.0F + exitAmt * SCALE_ADD + enterAmt * SCALE_EXIT;

        float xOff = exitAmt * ICON_OFFSET_X[slot - 1] + enterAmt * ICON_Y_OFFSET[slot - 1];
        float yOff = exitAmt * TRANSLATE_Y             + enterAmt * ICON_Y_OFFSET[slot - 1];

        float iconAlpha = alpha - exitAmt - enterAmt;
        float baseX = cx + (slot - 2) * RECT_ICON_FULL[2] - (float) RECT_ICON_FULL[2] / 2.0F;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(baseX + xOff + RECT_ICON_FULL[2] / 2.0F,
                by - BOTTOM_OFFSET + yOff + RECT_ICON_FULL[3] / 2.0F, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.pose().translate(-RECT_ICON_FULL[2] / 2.0F, -RECT_ICON_FULL[3] / 2.0F, 0);

        RenderSystem.setShaderColor(1f, 1f, 1f, Mth.clamp(iconAlpha, 0, 1));
        int[] rect = (slot <= current) ? RECT_ICON_FULL : RECT_ICON_EMPTY;
        blit(guiGraphics, 0, 0, rect[0], rect[1], rect[2], rect[3]);

        // Overlay de vaciado brillante
        if (slot == current + 1 && drainProgress > 0) {
            RenderSystem.setShaderColor(1f, 1f, 1f, (float) Math.sin(Math.PI * drainProgress) * 0.5F);
            blit(guiGraphics, 0, 0, RECT_ICON_EMPTY[0], RECT_ICON_EMPTY[1], RECT_ICON_EMPTY[2], RECT_ICON_EMPTY[3]);
        }

        guiGraphics.pose().popPose();
    }

    private static void blit(GuiGraphics guiGraphics, int x, int y, int u, int v, int w, int h) {
        // En 1.20.1 GuiGraphics maneja el texturizado directamente
        guiGraphics.blit(TEXTURE, x, y, u, v, w, h, 128, 128);
    }
}