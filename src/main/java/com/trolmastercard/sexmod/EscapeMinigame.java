package com.trolmastercard.sexmod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Random;

/**
 * HUD overlay for the directional-arrow escape minigame.
 * The player must press the shown arrow key quickly enough to escape.
 *
 * Key layout on the sprite sheet (at u=0, v=104 baseline):
 *   A key - (u=0,  v=104) 52-52
 *   S key - (u=52, v=104) 52-52
 *   D key - (u=156,v=104) 52-52
 *   W key - (u=0,  v=52 ) 52-52
 *   Progress bar - (u=152, v=8)  158-32
 *
 * Obfuscated name: gb
 */
@OnlyIn(Dist.CLIENT)
public class EscapeMinigame {

    static final ResourceLocation TEX =
            new ResourceLocation("sexmod", "textures/gui/escape_minigame_ui.png");

    static final float FILL_SPEED     = 0.08F;
    static final float DECAY_SPEED    = 0.006F;
    static final float CLOSE_SPEED    = 0.04F;
    static final int   ARROW_SIZE     = 52;
    static final int   ANIMATION_TICKS = 20;
    static final int   KEY_INTERVAL   = 35;

    static boolean active  = false;
    static Direction target = null;
    static float    fill    = 0.0F;
    static float    tick    = 0.0F;
    static boolean  shown   = true;
    static float    closeTick = 0.0F;
    static boolean  closing   = false;
    static boolean  sent      = false;

    static Minecraft mc = Minecraft.getInstance();

    // -- Direction enum --------------------------------------------------------
    enum Direction { A, S, D, W }

    // -- Public API ------------------------------------------------------------

    public static void activate() {
        active    = true;
        sent      = false;
        tick      = 0.0F;
        fill      = 0.0F;
        closeTick = 0.0F;
        closing   = false;
    }

    public static void startClosing() {
        closing   = true;
        closeTick = 0.0F;
    }

    public static boolean isActive() { return active; }

    // -- Internal tick ---------------------------------------------------------

    static void doTick() {
        if (!active) return;
        if (mc.level == null) { reset(); return; }

        if (closing) {
            shown = false;
            closeTick++;
            if (closeTick >= ANIMATION_TICKS) reset();
            return;
        }

        // Flicker "shown" every 2 ticks
        tick++;
        if (tick % Math.max(1, 2) == 0) shown = !shown;

        fill = Math.max(0.0F, fill - DECAY_SPEED);
        if (tick < ANIMATION_TICKS) return;

        // Cycle target arrow
        if (tick % KEY_INTERVAL == 0 || target == null) pickNewTarget();

        // Check win condition
        if (fill >= 1.0F) triggerEscape();
    }

    static void pickNewTarget() {
        Direction prev = target;
        Direction[] dirs = Direction.values();
        Random rng = new Random();
        do {
            target = dirs[rng.nextInt(dirs.length)];
        } while (target == prev);
    }

    static void triggerEscape() {
        if (sent) return;
        sent = true;
        ModNetwork.CHANNEL.sendToServer(new GalathBackOffPacket());
        startClosing();
    }

    static void reset() {
        active    = false;
        sent      = false;
        fill      = 0.0F;
        tick      = 0.0F;
        closeTick = 0.0F;
        closing   = false;
        target    = null;
        shown     = true;
    }

    // -- Events ----------------------------------------------------------------

    @SubscribeEvent
    public void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!active) return;
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.CHAT_PANEL.id())) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        // Sliding Y: rises in, falls out
        double slideY;
        if (closing) {
            double t = (closeTick + event.getPartialTick()) / ANIMATION_TICKS;
            slideY = easeIn(t);
        } else {
            double t = Math.min(1.0D, (tick + event.getPartialTick()) / ANIMATION_TICKS);
            slideY = 1.0D - easeOut(t);
        }

        int offY = (int)(h * slideY);
        int baseY = h - ARROW_SIZE * 4 + offY + h;   // below screen when offY==h

        RenderSystem.enableBlend();
        gfx.blit(TEX, w / 2 - 87, baseY, 0, 104,    104, 48);  // main bar
        gfx.blit(TEX, w / 2 - 78, baseY - ARROW_SIZE,  0, (shown && target == Direction.A) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);
        gfx.blit(TEX, w / 2 - 26, baseY - ARROW_SIZE, 52, (shown && target == Direction.S) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);
        gfx.blit(TEX, w / 2 + 26, baseY - ARROW_SIZE, 156,(shown && target == Direction.D) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);
        gfx.blit(TEX, w / 2 - 26, baseY - ARROW_SIZE * 2, 0, (shown && target == Direction.W) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);
        // Progress bar
        gfx.blit(TEX, w / 2 - 87 + 8, baseY - 8 + 8, 152, 8, (int)(158 * fill), 32);
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        doTick();
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (!active || closing) return;
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.client.KeyMapping[] keys = mc.options.keyMappings;

        // A
        if (isKeyDown(mc.options.keyLeft)  && target == Direction.A) fill += FILL_SPEED; else if (isKeyDown(mc.options.keyLeft))  fill -= CLOSE_SPEED;
        // D
        if (isKeyDown(mc.options.keyRight) && target == Direction.D) fill += FILL_SPEED; else if (isKeyDown(mc.options.keyRight)) fill -= CLOSE_SPEED;
        // W
        if (isKeyDown(mc.options.keyUp)    && target == Direction.W) fill += FILL_SPEED; else if (isKeyDown(mc.options.keyUp))    fill -= CLOSE_SPEED;
        // S
        if (isKeyDown(mc.options.keyDown)  && target == Direction.S) fill += FILL_SPEED; else if (isKeyDown(mc.options.keyDown))  fill -= CLOSE_SPEED;

        fill = net.minecraft.util.Mth.clamp(fill, 0.0F, 1.0F);
        if (fill >= 1.0F) triggerEscape();
    }

    private static boolean isKeyDown(net.minecraft.client.KeyMapping km) {
        return net.minecraft.client.KeyMapping.isDown(km.getKey().getValue());
    }

    // -- Math helpers ----------------------------------------------------------

    static double easeIn(double t)  { return t * t; }
    static double easeOut(double t) { return 1 - (1 - t) * (1 - t); }
}
