package com.trolmastercard.sexmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Full-screen animated transition/curtain overlay rendered during scene cuts.
 * Obfuscated name: fh
 */
@OnlyIn(Dist.CLIENT)
public class TransitionScreen {

    /** Duration of the transition in ticks (60 seconds at 20 tps). */
    public static final int DURATION_TICKS = 1200;

    private static boolean active = false;
    private static double  phase  = 0.0D;

    private static final ResourceLocation TEX_TRANSITION =
            new ResourceLocation("sexmod", "textures/gui/transitionscreen.png");
    private static final ResourceLocation TEX_MIRRORED   =
            new ResourceLocation("sexmod", "textures/gui/mirroredtransitionscreen.png");
    private static final ResourceLocation TEX_BLACK      =
            new ResourceLocation("sexmod", "textures/gui/blackscreen.png");

    public static boolean isActive() { return active; }

    /** Activate the transition without a follow-up action. */
    public static void activate() {
        active = true;
    }

    /** Activate the transition and schedule {@code action} via ModUtil after DURATION_TICKS. */
    public static void activate(Runnable action) {
        active = true;
        ModUtil.scheduleDelayed(DURATION_TICKS, action);
    }

    // -- Rendering -------------------------------------------------------------

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!active) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics gfx = event.getGuiGraphics();
        int guiScale = mc.options.guiScale().get();

        phase += mc.getDeltaFrameTime() * 0.75D;

        // x-position oscillates via cosine
        float xBase;
        if (guiScale == 1) {
            xBase = (float) MathUtil.lerp(-1800.0D, 1000.0D,
                    0.5D * Math.cos(phase / 25.0D) + 0.5D);
        } else if (guiScale == 2) {
            xBase = (float) MathUtil.lerp(-900.0D, 750.0D,
                    0.5D * Math.cos(phase / 25.0D) + 0.5D);
        } else {
            xBase = (float) MathUtil.lerp(-900.0D, 600.0D,
                    0.5D * Math.cos(phase / 25.0D) + 0.5D);
        }

        RenderSystem.enableBlend();

        if (guiScale == 1) RenderSystem.scaled(2.0, 2.0, 2.0);
        else if (guiScale == 2) RenderSystem.scaled(1.5, 1.5, 1.5);

        int scrollV = (int)(phase * 1.5D);

        // Left curtain strip (transition texture)
        renderStrip(gfx, TEX_TRANSITION, xBase,       0.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_TRANSITION, xBase,     256.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_TRANSITION, xBase,     512.0F, 0, scrollV, 256, 256);

        // Right curtain strip (mirrored)
        renderStrip(gfx, TEX_MIRRORED, xBase + 600.0F,   0.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_MIRRORED, xBase + 600.0F, 256.0F, 0, scrollV, 256, 256);
        renderStrip(gfx, TEX_MIRRORED, xBase + 600.0F, 512.0F, 0, scrollV, 256, 256);

        // Center black fill
        renderStrip(gfx, TEX_BLACK, xBase + 200.0F,   0.0F, 0, 0, 400, 256);
        renderStrip(gfx, TEX_BLACK, xBase + 200.0F, 256.0F, 0, 0, 400, 256);
        renderStrip(gfx, TEX_BLACK, xBase + 200.0F, 512.0F, 0, 0, 400, 256);

        // Trigger sex-UI fade at midpoint
        if (phase > 30.0D) HornyMeterOverlay.fadeOut();

        RenderSystem.disableBlend();

        // End of transition
        if (phase > 69.0D) {
            phase  = 0.0D;
            active = false;
        }
    }

    private void renderStrip(GuiGraphics gfx, ResourceLocation tex,
                              float x, float y, int u, int v, int w, int h) {
        gfx.blit(tex, (int) x, (int) y, u, v, w, h);
    }
}
