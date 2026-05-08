package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.util.ModConstants; // Importante para el Subscriber
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ModUtil — Portado a 1.20.1.
 * Utilidades matemáticas y programador de tareas sincronizado con los ticks del juego.
 */
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID)
public final class ModUtil {

    private static final Random RNG = new Random();
    private static final List<DelayedTask> TASKS = new CopyOnWriteArrayList<>();

    private ModUtil() {}

    // ── Tareas Programadas (Sincronizadas con el Reloj de MC) ────────────────

    /**
     * Ejecuta una acción después de N ticks (20 ticks = 1 segundo).
     * Es SEGURO para modificar el mundo, ya que corre en el hilo principal.
     */
    public static void scheduleDelayed(int ticks, Runnable action) {
        TASKS.add(new DelayedTask(ticks, action));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (DelayedTask task : TASKS) {
                task.delay--;
                if (task.delay <= 0) {
                    task.action.run();
                    TASKS.remove(task);
                }
            }
        }
    }

    private static class DelayedTask {
        int delay;
        Runnable action;
        DelayedTask(int delay, Runnable action) { this.delay = delay; this.action = action; }
    }

    // ── Ángulos y Rotaciones ─────────────────────────────────────────────────

    public static float angleDiff(double a, double b) {
        a = (a + Math.PI * 2) % (Math.PI * 2);
        b = (b + Math.PI * 2) % (Math.PI * 2);
        double d = b - a;
        while (d < -Math.PI) d += Math.PI * 2;
        while (d >= Math.PI) d -= Math.PI * 2;
        return (float) d;
    }

    public static YawPitch lookAngles(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        float yaw = (float) Math.atan2(dir.x, dir.z);
        float pitch = (float) Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
        return new YawPitch(yaw, pitch);
    }

    // ── Utilidades de Sistema ────────────────────────────────────────────────

    public static void copyToClipboard(String text) {
        if (FMLLoader.getDist() == Dist.CLIENT) {
            try {
                Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                cb.setContents(new StringSelection(text), null);
            } catch (Exception e) {
                System.err.println("[SexMod] Error al acceder al portapapeles: " + e.getMessage());
            }
        }
    }

    // ── Matemáticas y Helpers ────────────────────────────────────────────────

    public static boolean inRange(double value, double min, double max) {
        return value >= min && value < max;
    }

    public static float moveTowards(float current, float target, float step) {
        if (Math.abs(current - target) <= step) return target;
        return current < target ? current + step : current - step;
    }

    public static float clamp(float value, float min, float max) { return Mth.clamp(value, min, max); }
    public static double clamp(double value, double min, double max) { return Mth.clamp(value, min, max); }
}