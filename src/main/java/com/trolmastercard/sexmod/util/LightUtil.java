package com.trolmastercard.sexmod.util;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Random;
import java.util.UUID;

/**
 * LightUtil — Portado a 1.20.1 y optimizado.
 * * Colección de utilidades matemáticas, de sistema y de mundo.
 * Incluye: Cálculos de ángulos, manejo de portapapeles, hilos temporizados y detección de superficie.
 */
public final class LightUtil {

    private static final Random RNG = new Random();

    private LightUtil() {}

    // ── Matemáticas de Ángulos ────────────────────────────────────────────────

    /**
     * Calcula la diferencia angular más corta entre dos puntos en radianes.
     */
    public static float angleDelta(double from, double to) {
        double delta = (to - from) % (Math.PI * 2);
        if (delta < -Math.PI) delta += Math.PI * 2;
        if (delta >= Math.PI) delta -= Math.PI * 2;
        return (float) delta;
    }

    /**
     * Calcula el Yaw y Pitch necesarios para mirar desde un punto A a un punto B.
     */
    public static AngleTarget getHeadingTo(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        float yaw = (float) Mth.atan2(dir.x, dir.z);
        float pitch = (float) Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
        return new AngleTarget(yaw, pitch);
    }

    // ── Utilidades de Sistema ─────────────────────────────────────────────────

    /** Copia un texto al portapapeles del sistema operativo. */
    public static void copyToClipboard(String text) {
        try {
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            cb.setContents(new StringSelection(text), null);
        } catch (Exception e) {
            // Falla silenciosa si no hay entorno gráfico (servidores)
        }
    }

    /** Convierte "hola mundo" en "Hola mundo". */
    public static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ── Aleatoriedad y Rangos ─────────────────────────────────────────────────

    public static boolean inRange(double value, double min, double max) {
        return value >= min && value < max;
    }

    /** Distribución triangular: favorece los números más altos en el rango [0, n]. */
    public static int weightedRandom(int n) {
        if (n <= 0) return 0;
        int sum = n * (n + 1) / 2;
        int pick = RNG.nextInt(sum) + 1;
        int acc = 0;
        for (int i = 1; i <= n; i++) {
            acc += i;
            if (acc >= pick) return i;
        }
        return n;
    }

    public static int randomSign() {
        return RNG.nextBoolean() ? 1 : -1;
    }

    /** Uso de Mth.clamp para mayor rendimiento en 1.20.1 */
    public static float clamp(float val, float min, float max) {
        return Mth.clamp(val, min, max);
    }

    /**
     * Lógica de Deadzone/Snap: mueve un valor hacia un objetivo con un paso fijo.
     */
    public static float snapToTarget(float current, float target, float step) {
        if (Math.abs(current - target) <= step) return target;
        return current < target ? current + step : current - step;
    }

    // ── Gestión de Hilos (Threading) ──────────────────────────────────────────

    /**
     * Ejecuta una tarea después de un retraso sin bloquear el hilo principal.
     */
    public static void scheduleTask(int delayMs, Runnable task) {
        String threadName = "InteractionMod-Worker-" + UUID.randomUUID().toString().substring(0, 8);
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                task.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, threadName);
        t.setDaemon(true);
        t.start();
    }

    // ── Utilidades de Mundo ───────────────────────────────────────────────────

    /**
     * Obtiene la altura de la superficie (bloque sólido más alto) en X, Z.
     */
    public static int getSurfaceY(Level level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }
}