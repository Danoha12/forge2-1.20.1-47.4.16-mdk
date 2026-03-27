package com.trolmastercard.sexmod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * HUD de Felicidad / Amistad - Portado a 1.20.1
 * Este medidor muestra qué tan feliz está tu compañero actual.
 */
@OnlyIn(Dist.CLIENT)
public class HornyMeterOverlay extends GuiComponent {

    // -- Recursos de Textura --
    static final ResourceLocation BUTTONS_TEX = new ResourceLocation("sexmod", "textures/gui/buttons.png");
    static final ResourceLocation HAPPY_METER_TEX = new ResourceLocation("sexmod", "textures/gui/hornymeter.png");

    // -- Estado Compartido --
    public static boolean visible = false;
    public static double progress = 0.0;             // Progreso de 0.0 a 1.0
    static double smoothProgress = progress;
    static float slideIn = 0.0F;                     // Animación de entrada
    static float rewardSlide = 0.0F;                 // Animación cuando llega al máximo
    static boolean maxHappinessReached = false;
    static boolean showButtons = true;

    // -- API Pública --

    public static void showWithButtons() {
        if (visible) return;
        reset();
        visible = true;
        showButtons = true;
    }

    public static void hide() {
        reset();
        visible = false;
    }

    public static void addProgress(double delta) {
        progress = Math.min(1.0, progress + delta);
    }

    public static void reset() {
        progress = 0.0;
        maxHappinessReached = false;
        slideIn = 0.0F;
        rewardSlide = 0.0F;
    }

    // -- Renderizado --

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!visible) return;

        // Solo dibujamos si el F1 no está activado y estamos en la mira normal
        if (event.getOverlay() != VanillaGuiOverlay.CROSSHAIR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        float partialTicks = event.getPartialTick();

        // Actualizar animación de entrada deslizante
        if (slideIn < 1.0F) {
            slideIn = Math.min(1.0F, slideIn + partialTicks / 25.0F);
        }

        poseStack.pushPose();

        // Dibujar botones de acción (si están activos)
        RenderSystem.setShaderTexture(0, BUTTONS_TEX);
        poseStack.pushPose();
        poseStack.scale(0.35F, 0.35F, 0.35F);

        if (progress >= 1.0) {
            // Aquí podrías detectar una tecla para dar una recompensa
            maxHappinessReached = true;
            int btnX = maxHappinessReached ? 54 : 0;
            blit(poseStack, 240, 160, 0, 108 + btnX, 256, 52, 256, 256);
        }

        if (showButtons && !maxHappinessReached) {
            // Deslizar desde la izquierda (-200 a 98)
            int btnX = (int) ( -200.0F + (298.0F * slideIn) );
            blit(poseStack, btnX, 405, 0, 0, 158, 54, 256, 256);
        }
        poseStack.popPose();

        // --- Dibujar Medidor de Felicidad ---
        RenderSystem.setShaderTexture(0, HAPPY_METER_TEX);
        poseStack.pushPose();
        poseStack.scale(0.75F, 0.75F, 0.75F);

        // Fondo del medidor (Desliza desde arriba)
        int meterY = (int) ( -200.0F + (210.0F * slideIn) );
        blit(poseStack, 10, meterY, 0, 0, 146, 175, 256, 256);

        // Suavizar el movimiento de la barra
        smoothProgress = smoothProgress + (progress - smoothProgress) * 0.1;

        int fillHeight  = (int) (160.0 * smoothProgress);
        int fillSrcY    = (int) (167.0 - (159.0 * smoothProgress));
        int fillDestY   = meterY + 8 + (160 - fillHeight);

        if (!maxHappinessReached) {
            // Relleno de la barra (Color rosa/fuerte)
            blit(poseStack, 67, fillDestY, 159, fillSrcY, 32, fillHeight, 256, 256);

            // Adornos / Flores laterales que suben con el progreso
            int iconY = meterY + 150 - (int)(140 * smoothProgress);
            blit(poseStack, 120, iconY, 212, 141, 28, 29, 256, 256);
            blit(poseStack, 18, iconY, 212, 141, 28, 29, 256, 256);
        } else {
            // Animación cuando está al máximo
            rewardSlide += partialTicks / 15.0F;
            int exitY = (int) ( (meterY + 8) - (300.0F * rewardSlide) );
            blit(poseStack, 67, exitY, 159, 8, 32, 160, 256, 256);
        }

        poseStack.popPose();
        poseStack.popPose();
    }

    // -- Alias para compatibilidad con otras entidades --

    public static void onInteractionStart() { show(true); }

    public static void setVisible(boolean v) { if (v) show(false); else hide(); }

    public static void addValue(double delta) { addProgress(delta); }

    public static void show(boolean showButtonBar) {
        if (visible) return;
        reset();
        visible = true;
        showButtons = showButtonBar;
    }
}