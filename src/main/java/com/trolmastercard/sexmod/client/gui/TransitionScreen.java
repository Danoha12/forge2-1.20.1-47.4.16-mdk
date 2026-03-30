package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.ModConstants;
import com.trolmastercard.sexmod.util.ModUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * TransitionScreen — Portado a 1.20.1.
 * * Pantalla de transición animada (telón) para cortes de escena.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TransitionScreen {

    public static final int DURATION_TICKS = 1200;

    private static boolean active = false;
    private static double phase = 0.0D;

    private static final ResourceLocation TEX_TRANSITION = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/transitionscreen.png");
    private static final ResourceLocation TEX_MIRRORED = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/mirroredtransitionscreen.png");
    private static final ResourceLocation TEX_BLACK = new ResourceLocation(ModConstants.MOD_ID, "textures/gui/blackscreen.png");

    public static boolean isActive() { return active; }

    /** Activa la transición sin acción posterior. (Renombrado para coincidir con LunaEntity) */
    public static void show() {
        active = true;
    }

    /** Activa la transición y programa una acción diferida. */
    public static void show(Runnable action) {
        active = true;
        ModUtil.scheduleDelayed(DURATION_TICKS, action);
    }

    // ── Renderizado ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!active) return;

        // Renderizar solo una vez por frame (después de dibujar el panel de chat)
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics gfx = event.getGuiGraphics();
        int guiScale = mc.options.guiScale().get();

        // Incremento de fase basado en el framerate
        float deltaFrame = mc.getDeltaFrameTime(); // Si no compila en tu mapping, usa event.getPartialTick()
        phase += deltaFrame * 0.75D;

        // Oscilación de posición X vía coseno
        float xBase;
        if (guiScale == 1) {
            xBase = (float) MathUtil.lerp(-1800.0D, 1000.0D, 0.5D * Math.cos(phase / 25.0D) + 0.5D);
        } else if (guiScale == 2) {
            xBase = (float) MathUtil.lerp(-900.0D, 750.0D, 0.5D * Math.cos(phase / 25.0D) + 0.5D);
        } else {
            xBase = (float) MathUtil.lerp(-900.0D, 600.0D, 0.5D * Math.cos(phase / 25.0D) + 0.5D);
        }

        RenderSystem.enableBlend();

        // ── USO DE POSESTACK EN LUGAR DE RENDERSYSTEM SCALED ──
        gfx.pose().pushPose();

        if (guiScale == 1) {
            gfx.pose().scale(2.0F, 2.0F, 2.0F);
        } else if (guiScale == 2) {
            gfx.pose().scale(1.5F, 1.5F, 1.5F);
        }

        int scrollV = (int) (phase * 1.5D);

        // Cortina Izquierda (Textura normal)
        renderStrip(gfx, TEX_TRANSITION, xBase, 0.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_TRANSITION, xBase, 256.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_TRANSITION, xBase, 512.0F, 0, scrollV, 256, 256);

        // Cortina Derecha (Espejeada)
        renderStrip(gfx, TEX_MIRRORED, xBase + 600.0F, 0.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_MIRRORED, xBase + 600.0F, 256.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_MIRRORED, xBase + 600.0F, 512.0F, 0, scrollV, 256, 256);

        // Centro (Fondo Negro)
        renderStrip(gfx, TEX_BLACK, xBase + 200.0F, 0.0F, 0, 0, 400, 256);
        renderStrip(gfx, TEX_BLACK, xBase + 200.0F, 256.0F, 0, 0, 400, 256);
        renderStrip(gfx, TEX_BLACK, xBase + 200.0F, 512.0F, 0, 0, 400, 256);

        gfx.pose().popPose(); // Restauramos la escala original para no romper el resto de la UI

        // Disparar fade out del UI a la mitad
        if (phase > 30.0D) {
            HornyMeterOverlay.fadeOut();
        }

        RenderSystem.disableBlend();

        // Fin de la transición
        if (phase > 69.0D) {
            phase = 0.0D;
            active = false;
        }
    }

    private static void renderStrip(GuiGraphics gfx, ResourceLocation tex, float x, float y, int u, int v, int w, int h) {
        gfx.blit(tex, (int) x, (int) y, u, v, w, h);
    }
}