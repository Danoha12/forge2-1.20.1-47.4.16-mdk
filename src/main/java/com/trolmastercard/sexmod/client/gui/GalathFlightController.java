package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * GalathFlightController — Portado a 1.20.1.
 * * Renderiza el HUD de cargas de vuelo (3 íconos) durante la secuencia de Galath.
 * * Maneja animaciones de entrada/salida (Fade), regeneración y consumo.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class GalathFlightController {

    static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/gui/galath_flight_ui.png");

    // Rectángulos (u, v, w, h)
    static final int[] RECT_BG         = { 0, 77, 128, 41 };
    static final int[] RECT_ICON_FULL  = { 0,  0,  23, 36 };
    static final int[] RECT_ICON_EMPTY = { 0, 36,  23, 36 };
    static final int[] RECT_CONNECTOR  = { 23, 2,  20, 31 };

    static final long REGEN_DELAY_MS    = 3000L;
    static final long REGEN_COOLDOWN_MS = 5000L;
    static final long FADE_DURATION     = 500L;

    static final float[] ICON_OFFSET_X = { -14.25F, -15.5F, -16.875F };
    static final float TRANSLATE_Y     = -11.25F;
    static final float SCALE_ADD       = 0.075F;

    static final int BOTTOM_OFFSET = 70;
    static final int MAX_CHARGES   = 3;

    // ── Estado Estático ───────────────────────────────────────────────────────
    static boolean visible    = false;
    static int charges        = 3;
    static long lastUsedTime  = 0L;
    static long lastRegenTime = 0L;
    static long fadeInStart   = 0L;
    static long fadeOutStart  = Long.MAX_VALUE - 500L;

    // ── API Pública ───────────────────────────────────────────────────────────

    public static boolean canUseCharge() {
        return charges > 0 && (System.currentTimeMillis() - lastUsedTime > REGEN_DELAY_MS);
    }

    public static void useCharge() {
        charges--;
        lastUsedTime = System.currentTimeMillis();
    }

    public static void show() {
        if (visible) return;
        visible = true;
        fadeInStart = System.currentTimeMillis();
        fadeOutStart = Long.MAX_VALUE - 500L;
    }

    public static void startFadeOut() {
        fadeOutStart = System.currentTimeMillis();
    }

    public static void hide() {
        visible = false;
        fadeOutStart = Long.MAX_VALUE - 500L;
        fadeInStart = 0L;
    }

    // ── Registro del Overlay (Forge 1.20.1) ───────────────────────────────────

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // Registramos el HUD después del de "Air" (burbujas) para que no se tapen
        event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), "galath_flight_hud", OVERLAY);
    }

    // ── Implementación del IGuiOverlay ────────────────────────────────────────

    public static final IGuiOverlay OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        tickRegen();
        if (!visible) return;

        long now = System.currentTimeMillis();

        if (now - fadeOutStart > FADE_DURATION) {
            hide();
            return;
        }

        // Cálculo de Alpha para el fundido (Fade)
        float alpha = 1.0F;
        if (now < fadeInStart + FADE_DURATION) {
            alpha = (float) (now - fadeInStart) / FADE_DURATION;
        } else if (now < fadeOutStart + FADE_DURATION) {
            alpha = 1.0F - (float) (now - fadeOutStart) / FADE_DURATION;
        }
        alpha = Mth.clamp(alpha, 0.0F, 1.0F);

        int cx = width / 2;
        int by = height - BOTTOM_OFFSET;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. Fondo de la barra
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(TEXTURE, cx - RECT_BG[2] / 2, by - RECT_BG[3], RECT_BG[0], RECT_BG[1], RECT_BG[2], RECT_BG[3], 128, 128);

        // 2. Conectores entre íconos
        guiGraphics.blit(TEXTURE, (int) (cx - 1.5F * RECT_ICON_FULL[2] + 1), by - BOTTOM_OFFSET + 3, RECT_CONNECTOR[0], RECT_CONNECTOR[1], RECT_CONNECTOR[2], RECT_CONNECTOR[3], 128, 128);
        guiGraphics.blit(TEXTURE, cx - RECT_ICON_FULL[2] / 2 + 1, by - BOTTOM_OFFSET + 3, RECT_CONNECTOR[0], RECT_CONNECTOR[1], RECT_CONNECTOR[2], RECT_CONNECTOR[3], 128, 128);
        guiGraphics.blit(TEXTURE, cx + RECT_ICON_FULL[2] / 2 + 1, by - BOTTOM_OFFSET + 3, RECT_CONNECTOR[0], RECT_CONNECTOR[1], RECT_CONNECTOR[2], RECT_CONNECTOR[3], 128, 128);

        // 3. Progreso de drenado (Efecto visual cuando acabas de usar una carga)
        float drainProgress = charges > 0 ? (float) Math.sin(Math.PI * Mth.clamp(1.0F - (float) (now - lastUsedTime) / 500.0F, 0, 1)) * 0.5F : 0.0F;

        // 4. Íconos de Carga
        for (int i = 1; i <= MAX_CHARGES; i++) {
            renderChargeIcon(guiGraphics, i, charges, drainProgress, cx, by, alpha);
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    };

    private static void renderChargeIcon(GuiGraphics guiGraphics, int slot, int current, float drainProgress, int cx, int by, float alpha) {
        float exitAmt = (slot == current + 1) ? drainProgress : 0.0F;
        float scale = 1.0F + exitAmt * SCALE_ADD;
        float xOff = exitAmt * ICON_OFFSET_X[slot - 1];
        float yOff = exitAmt * TRANSLATE_Y;
        float iconAlpha = Mth.clamp(alpha - exitAmt, 0, 1);

        float baseX = cx + (slot - 2) * RECT_ICON_FULL[2] - (float) RECT_ICON_FULL[2] / 2.0F;

        // En 1.20.1, las transformaciones se aplican al PoseStack dentro de GuiGraphics
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(baseX + xOff + RECT_ICON_FULL[2] / 2.0F, (by - BOTTOM_OFFSET) + yOff + RECT_ICON_FULL[3] / 2.0F, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.pose().translate(-RECT_ICON_FULL[2] / 2.0F, -RECT_ICON_FULL[3] / 2.0F, 0);

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, iconAlpha);
        int[] rect = (slot <= current) ? RECT_ICON_FULL : RECT_ICON_EMPTY;
        guiGraphics.blit(TEXTURE, 0, 0, rect[0], rect[1], rect[2], rect[3], 128, 128);

        // Overlay de carga agotándose (Parpadeo)
        if (slot == current + 1 && drainProgress > 0) {
            float pulse = (float) Math.sin(Math.PI * drainProgress) * 0.5F;
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, pulse * alpha);
            guiGraphics.blit(TEXTURE, 0, 0, RECT_ICON_EMPTY[0], RECT_ICON_EMPTY[1], RECT_ICON_EMPTY[2], RECT_ICON_EMPTY[3], 128, 128);
        }

        guiGraphics.pose().popPose();
    }

    private static void tickRegen() {
        if (charges == MAX_CHARGES) return;
        long now = System.currentTimeMillis();
        if (now - Math.max(lastUsedTime, lastRegenTime) < REGEN_COOLDOWN_MS) return;
        charges++;
        lastRegenTime = now;
    }
}