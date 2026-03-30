package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.client.handler.ClientStateManager;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * HornyMeterOverlay — Portado a 1.20.1.
 * * Renderiza la barra de progreso lateral y los botones de acción.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HornyMeterOverlay {

    private static final ResourceLocation BUTTONS_TEX = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/buttons.png");
    private static final ResourceLocation METER_TEX = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/hornymeter.png");

    // ── Estado Global ────────────────────────────────────────────────────────
    public static boolean visible = false;
    public static double interactionProgress = 0.0;

    private static double smoothProgress = 0.0;
    private static float slideInFactor = 0.0f;
    private static float flyOutFactor = 0.0f;
    private static boolean isFinishing = false;
    private static boolean showButtons = true;

    // ── API Pública ──────────────────────────────────────────────────────────

    public static void show() {
        if (visible) return;
        reset();
        visible = true;
        showButtons = true;
        slideInFactor = 0.0f;
    }

    public static void hide() {
        visible = false;
        reset();
    }

    public static void fadeOut() {
        hide();
    }

    public static void addHorny(double amount) {
        interactionProgress = Mth.clamp(interactionProgress + amount, 0.0, 1.0);
    }

    public static void reset() {
        interactionProgress = 0.0;
        smoothProgress = 0.0;
        isFinishing = false;
        flyOutFactor = 0.0f;
    }

    // Alias para compatibilidad con la clase TransitionScreen y entidades
    public static void showSexUI() {
        show();
    }

    // ── Renderizado ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!visible) return;

        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics gg = event.getGuiGraphics();
        float deltaFrame = mc.getDeltaFrameTime();

        if (slideInFactor < 1.0f) {
            slideInFactor = Math.min(1.0f, slideInFactor + (deltaFrame / 20.0f));
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        var pose = gg.pose();
        pose.pushPose();
        RenderSystem.enableBlend();

        if (showButtons && !isFinishing) {
            pose.pushPose();
            pose.scale(0.5f, 0.5f, 0.5f);

            boolean isSneaking = ClientStateManager.sneakPressed;
            int btnX = (int) Mth.lerp(slideInFactor, -200, 20);
            int btnY = (screenHeight * 2) - 120;

            gg.blit(BUTTONS_TEX, btnX, btnY, 0, isSneaking ? 54 : 0, 158, 54, 256, 256);

            if (interactionProgress >= 1.0) {
                if (ClientStateManager.jumpPressed) isFinishing = true;
                int flashV = (mc.level != null && mc.level.getGameTime() % 10 < 5) ? 108 : 162;
                gg.blit(BUTTONS_TEX, (screenWidth * 2) - 280, btnY, 0, flashV, 256, 52, 256, 256);
            }
            pose.popPose();
        }

        pose.pushPose();
        pose.scale(0.75f, 0.75f, 0.75f);

        smoothProgress = Mth.lerp(deltaFrame * 0.1f, (float) smoothProgress, (float) interactionProgress);

        int meterX = 10;
        int meterY = (int) Mth.lerp(slideInFactor, -200, 20);

        gg.blit(METER_TEX, meterX, meterY, 0, 0, 146, 175, 256, 256);

        int fillHeight = (int) (smoothProgress * 160);
        int srcY = 167 - fillHeight;
        int destY = (meterY + 172) - fillHeight;

        if (!isFinishing) {
            gg.blit(METER_TEX, meterX + 57, destY, 159, srcY, 32, fillHeight, 256, 256);
        } else {
            flyOutFactor += deltaFrame / 10.0f;
            int flyY = destY - (int) (flyOutFactor * 300);
            gg.blit(METER_TEX, meterX + 57, flyY, 159, 8, 32, 160, 256, 256);

            if (flyOutFactor > 2.0f) hide();
        }

        pose.popPose();
        RenderSystem.disableBlend();
        pose.popPose();
    }
}