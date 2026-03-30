package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.registry.YawPitch; // Asumiendo que esta clase existe
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Random;
import java.util.UUID;

/**
 * ModUtil — Portado a 1.20.1.
 * * Utilidades generales para cálculos matemáticos, portapapeles y tareas programadas.
 */
public final class ModUtil {

    private static final Random RNG = new Random();
    private static final double TO_RAD = Math.PI / 180.0D;

    private ModUtil() {}

    // ── Ángulos y Rotaciones ─────────────────────────────────────────────────

    /** Calcula la diferencia más corta entre dos ángulos en radianes. */
    public static float angleDiff(double a, double b) {
        a = (a + Math.PI * 2) % (Math.PI * 2);
        b = (b + Math.PI * 2) % (Math.PI * 2);
        double d = b - a;
        while (d < -Math.PI) d += Math.PI * 2;
        while (d >= Math.PI) d -= Math.PI * 2;
        return (float) d;
    }

    /** Calcula Yaw y Pitch necesarios para mirar desde un punto a otro. */
    public static YawPitch lookAngles(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        float yaw = (float) Math.atan2(dir.x, dir.z);
        float pitch = (float) Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
        return new YawPitch(yaw, pitch);
    }

    // ── Utilidades de Sistema (Solo Cliente) ─────────────────────────────────

    /** Copia texto al portapapeles. Protegido para no crashear servidores dedicados. */
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

    // ── Manipulación de Strings ─────────────────────────────────────────────

    public static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // ── Matemáticas y Rangos ────────────────────────────────────────────────

    public static boolean inRange(double value, double min, double max) {
        return value >= min && value < max;
    }

    /** Genera un número aleatorio con distribución triangular (preferencia a valores altos). */
    public static int weightedRandom(int n) {
        if (n <= 0) return n;
        int total = (n * (n + 1)) / 2;
        int pick = RNG.nextInt(total) + 1;
        int cumul = 0;
        for (int k = 1; k <= n; k++) {
            cumul += k;
            if (cumul >= pick) return k;
        }
        return n;
    }

    public static int randomSign() {
        return RNG.nextBoolean() ? 1 : -1;
    }

    public static float clamp(float value, float min, float max) {
        return Mth.clamp(value, min, max);
    }

    public static double clamp(double value, double min, double max) {
        return Mth.clamp(value, min, max);
    }

    public static float randomFloat(float range, boolean signed) {
        float v = RNG.nextFloat() * range;
        if (signed && RNG.nextBoolean()) v = -v;
        return v;
    }

    /** Acerca un valor actual hacia un objetivo por pasos (útil para suavizar animaciones). */
    public static float moveTowards(float current, float target, float step) {
        if (Math.abs(current - target) <= step) return target;
        if (Math.abs(current) < Math.abs(target)) {
            return target > 0 ? target - step : target + step;
        }
        return current > 0 ? current - step : current + step;
    }

    public static int roundToInt(double d) {
        return Mth.floor(d + 0.5D);
    }

    // ── Tareas Programadas ──────────────────────────────────────────────────

    /** * Ejecuta una tarea después de X milisegundos.
     * ¡OJO!: Si la tarea toca el mundo de MC, debe sincronizarse con el hilo principal.
     */
    public static void scheduleTask(int millis, Runnable task) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String side = FMLLoader.getDist().isClient() ? "Client" : "Server";

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(millis);
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "SexMod-Task-" + side + "-" + id);

        t.setDaemon(true);
        t.start();
    }
}