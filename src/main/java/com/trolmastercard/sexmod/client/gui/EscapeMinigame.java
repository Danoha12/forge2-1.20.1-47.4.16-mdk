package com.trolmastercard.sexmod.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.GalathBackOffPacket; // Asumiendo que este es tu paquete
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

/**
 * EscapeMinigame — Portado a 1.20.1.
 * * Minijuego de QTE donde el jugador debe machacar WASD para escapar.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EscapeMinigame {

    private static final ResourceLocation TEX = new ResourceLocation("sexmod", "textures/gui/escape_minigame_ui.png");

    static final float FILL_SPEED = 0.08F;
    static final float DECAY_SPEED = 0.006F;
    static final float CLOSE_SPEED = 0.04F;
    static final int ARROW_SIZE = 52;
    static final int ANIMATION_TICKS = 20;
    static final int KEY_INTERVAL = 35;

    static boolean active = false;
    static Direction target = null;
    static float fill = 0.0F;
    static float tick = 0.0F;
    static boolean shown = true;
    static float closeTick = 0.0F;
    static boolean closing = false;
    static boolean sent = false;

    enum Direction { A, S, D, W }

    // ── API Pública ──────────────────────────────────────────────────────────

    public static void activate() {
        active = true;
        sent = false;
        tick = 0.0F;
        fill = 0.0F;
        closeTick = 0.0F;
        closing = false;
    }

    public static void startClosing() {
        closing = true;
        closeTick = 0.0F;
    }

    public static boolean isActive() { return active; }

    static void reset() {
        active = false;
        sent = false;
        fill = 0.0F;
        tick = 0.0F;
        closeTick = 0.0F;
        closing = false;
        target = null;
        shown = true;
    }

    // ── Lógica Interna ───────────────────────────────────────────────────────

    static void triggerEscape() {
        if (sent) return;
        sent = true;
        ModNetwork.CHANNEL.sendToServer(new GalathBackOffPacket());
        startClosing();
    }

    static void pickNewTarget() {
        Direction prev = target;
        Direction[] dirs = Direction.values();
        Random rng = new Random();
        do {
            target = dirs[rng.nextInt(dirs.length)];
        } while (target == prev);
    }

    // ── Renderizado del HUD (Estándar 1.20.1) ────────────────────────────────

    public static final IGuiOverlay HUD_OVERLAY = (ForgeGui gui, GuiGraphics gfx, float partialTick, int width, int height) -> {
        if (!active) return;

        double slideY;
        if (closing) {
            double t = (closeTick + partialTick) / ANIMATION_TICKS;
            slideY = easeIn(t);
        } else {
            double t = Math.min(1.0D, (tick + partialTick) / ANIMATION_TICKS);
            slideY = 1.0D - easeOut(t);
        }

        int offY = (int) (height * slideY);
        int baseY = height - ARROW_SIZE * 4 + offY + height;

        RenderSystem.enableBlend();

        // gfx.blit(Textura, X, Y, U, V, Ancho, Alto)
        gfx.blit(TEX, width / 2 - 87, baseY, 0, 104, 104, 48); // Barra principal

        // Flechas
        gfx.blit(TEX, width / 2 - 78, baseY - ARROW_SIZE, 0, (shown && target == Direction.A) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);
        gfx.blit(TEX, width / 2 - 26, baseY - ARROW_SIZE, 52, (shown && target == Direction.S) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);
        gfx.blit(TEX, width / 2 + 26, baseY - ARROW_SIZE, 156, (shown && target == Direction.D) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);
        gfx.blit(TEX, width / 2 - 26, baseY - ARROW_SIZE * 2, 0, (shown && target == Direction.W) ? ARROW_SIZE : 0, ARROW_SIZE, ARROW_SIZE);

        // Progreso
        gfx.blit(TEX, width / 2 - 87 + 8, baseY - 8 + 8, 152, 8, (int) (158 * fill), 32);

        RenderSystem.disableBlend();
    };

    // ── Eventos ──────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END || !active) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) { reset(); return; }

        if (closing) {
            shown = false;
            closeTick++;
            if (closeTick >= ANIMATION_TICKS) reset();
            return;
        }

        tick++;
        if (tick % 2 == 0) shown = !shown; // Parpadeo

        fill = Math.max(0.0F, fill - DECAY_SPEED); // Decaimiento
        if (tick < ANIMATION_TICKS) return;

        if (tick % KEY_INTERVAL == 0 || target == null) pickNewTarget();

        if (fill >= 1.0F) triggerEscape();
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!active || closing) return;

        // Solo registrar la pulsación inicial (evita que llenen la barra dejando la tecla apretada)
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        int key = event.getKey();

        // Comparamos el botón pulsado con los KeyMappings (WASD)
        if (key == mc.options.keyLeft.getKey().getValue()) {
            if (target == Direction.A) fill += FILL_SPEED; else fill -= CLOSE_SPEED;
        } else if (key == mc.options.keyRight.getKey().getValue()) {
            if (target == Direction.D) fill += FILL_SPEED; else fill -= CLOSE_SPEED;
        } else if (key == mc.options.keyUp.getKey().getValue()) {
            if (target == Direction.W) fill += FILL_SPEED; else fill -= CLOSE_SPEED;
        } else if (key == mc.options.keyDown.getKey().getValue()) {
            if (target == Direction.S) fill += FILL_SPEED; else fill -= CLOSE_SPEED;
        }

        fill = Mth.clamp(fill, 0.0F, 1.0F);
        if (fill >= 1.0F) triggerEscape();
    }

    // ── Helpers Matemáticos ──────────────────────────────────────────────────
    static double easeIn(double t) { return t * t; }
    static double easeOut(double t) { return 1 - (1 - t) * (1 - t); }
}