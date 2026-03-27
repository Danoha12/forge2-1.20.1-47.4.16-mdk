package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.ModConstants;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLLoader;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Random;
import java.util.UUID;

/**
 * ModUtil - ported from be.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * General-purpose static utility methods used throughout the mod.
 * Originally named {@code be} with entirely obfuscated method names.
 *
 * Method mapping:
 *   a(double,double)          - angleDiff(double,double)       - radian shortest-path difference
 *   a(Vec3d,Vec3d)            - lookAngles(Vec3,Vec3)          - YawPitch
 *   a(String)                 - copyToClipboard(String)
 *   b(String)                 - capitalizeFirst(String)
 *   a(double,double,double)   - inRange(double,double,double)  - [min, max)
 *   a(int)                    - weightedRandom(int)            - triangular-weighted
 *   a()                       - randomSign()                   - +1 or -1
 *   b(float,float,float)      - clamp(float,float,float)
 *   b(double,double,double)   - clamp(double,double,double)
 *   a(float,boolean)          - randomFloat(float,boolean)
 *   a(float,float,float) x3f  - moveTowards(float,float,float)- step-towards with step size
 *   a(double)                 - roundToInt(double)
 *   a(int,Runnable)           - scheduleTask(int millis, Runnable)
 *
 * In 1.12.2:
 *   {@code g0.a()} (was isServerRunning / side check) -
 *     {@code !FMLLoader.isProduction() || Thread.currentThread().getName().contains("server")}
 *   Thread name prefix used to be "server sexmod thread" / "client sexmod thread"
 */
public final class ModUtil {

    private ModUtil() {}

    // =========================================================================
    //  a(double, double) - angleDiff
    // =========================================================================

    /**
     * Computes the signed shortest-arc angular difference from {@code a} to {@code b}
     * in radians.  Both inputs are normalised to [0, 2-) before diffing.
     *
     * Original: {@code be.a(double, double)}
     */
    public static float angleDiff(double a, double b) {
        a = (a + 2 * Math.PI) % (2 * Math.PI);
        b = (b + 2 * Math.PI) % (2 * Math.PI);
        double d = b - a;
        while (d < -Math.PI) d += 2 * Math.PI;
        while (d >= Math.PI)  d -= 2 * Math.PI;
        return (float) d;
    }

    // =========================================================================
    //  a(Vec3d, Vec3d) - lookAngles - YawPitch
    // =========================================================================

    /**
     * Returns the yaw and pitch (radians) needed to look from {@code from} toward {@code to}.
     *
     * Original: {@code be.a(Vec3d, Vec3d)} - {@code new bm(yaw, pitch)}
     */
    public static YawPitch lookAngles(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        float yaw   = (float) Math.atan2(dir.x, dir.z);
        float pitch = (float) Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z));
        return new YawPitch(yaw, pitch);
    }

    // =========================================================================
    //  a(String) - copyToClipboard
    // =========================================================================

    /** Copies {@code text} to the OS system clipboard. Original: {@code be.a(String)} */
    public static void copyToClipboard(String text) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new StringSelection(text), null);
    }

    // =========================================================================
    //  b(String) - capitalizeFirst
    // =========================================================================

    /**
     * Returns {@code s} with its first character upper-cased and the rest lower-cased.
     * Returns {@code s} unchanged if null or empty.
     *
     * Original: {@code be.b(String)}
     */
    public static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // =========================================================================
    //  a(double, double, double) - inRange
    // =========================================================================

    /**
     * Returns true iff {@code value} is in the half-open interval [{@code min}, {@code max}).
     *
     * Original: {@code be.a(double, double, double)}
     */
    public static boolean inRange(double value, double min, double max) {
        if (value < min)  return false;
        if (value >= max) return false;
        return true;
    }

    // =========================================================================
    //  a(int) - weightedRandom
    // =========================================================================

    /**
     * Returns a random integer in [0, n] with a triangular distribution
     * (lower values are rarer than the mode).
     *
     * Algorithm: generate cumulative triangle weights T(k)=k*(k+1)/2, pick
     * a random in [1, T(n)] and return the first k where T(k) >= pick.
     *
     * Original: {@code be.a(int)}
     */
    public static int weightedRandom(int n) {
        if (n <= 0) return n;
        Random rng = new Random();
        int total = 0;
        for (int j = 0; j <= n; j++) total += j;
        int pick  = rng.nextInt(total) + 1;
        int cumul = 0;
        for (int k = 0; k <= n; k++) {
            cumul += k;
            if (cumul >= pick) return k;
        }
        return n;
    }

    // =========================================================================
    //  a() - randomSign
    // =========================================================================

    /**
     * Returns +1 or -1 with equal probability.
     *
     * Original: {@code be.a()} using {@code r.f.nextBoolean()}
     */
    public static int randomSign() {
        return ModConstants.RAND.nextBoolean() ? 1 : -1;
    }

    // =========================================================================
    //  b(float, float, float) - clamp(float)
    //  b(double, double, double) - clamp(double)
    // =========================================================================

    /** Clamps {@code value} into [min, max]. Original: {@code be.b(float,float,float)} */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Clamps {@code value} into [min, max]. Original: {@code be.b(double,double,double)} */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // =========================================================================
    //  a(float, boolean) - randomFloat
    // =========================================================================

    /**
     * Returns a random float in [0, range] (always positive).
     * If {@code signed} is true, has a 50% chance of being negated.
     *
     * Original: {@code be.a(float, boolean)} - note: decompile suggests the negation
     * logic compiled oddly; the intent is:
     *   result = rng.nextFloat() * range
     *   if (signed && rng.nextBoolean()) result = -result
     *   return result
     */
    public static float randomFloat(float range, boolean signed) {
        Random rng = new Random();
        float v = rng.nextFloat() * range;
        if (signed && rng.nextBoolean()) v = -v;
        return v;
    }

    // =========================================================================
    //  a(float, float, float) [three floats] - moveTowards
    // =========================================================================

    /**
     * Steps {@code current} toward {@code target} by at most {@code step}.
     *
     * If the distance to target is - step, returns current unchanged.
     * Otherwise:
     *   - if |current| < |target|: steps away from zero (reduces the gap from the far side)
     *   - if current > 0: subtracts step
     *   - if current < 0: adds step
     *
     * Original: {@code be.a(float current, float target, float step)}
     * Used to smoothly approach a target value each tick.
     */
    public static float moveTowards(float current, float target, float step) {
        if (Math.abs(current - target) <= step) return current;
        if (Math.abs(current) < Math.abs(target)) {
            return target > 0 ? target - step : target + step;
        }
        return current > 0 ? current - step : current + step;
    }

    // =========================================================================
    //  a(double) - roundToInt
    // =========================================================================

    /** Rounds {@code d} to the nearest integer. Original: {@code be.a(double)} */
    public static int roundToInt(double d) {
        return Math.round((float) d);
    }

    // =========================================================================
    //  a(int, Runnable) - scheduleTask
    // =========================================================================

    /**
     * Schedules {@code task} to run after {@code millis} milliseconds on a new daemon thread.
     * Thread is named "server sexmod thread &lt;uuid&gt;" or "client sexmod thread &lt;uuid&gt;"
     * depending on the current side.
     *
     * Original: {@code be.a(int, Runnable)}
     */
    public static void scheduleTask(int millis, Runnable task) {
        String id     = UUID.randomUUID().toString();
        boolean server = isServerThread();
        String name   = (server ? "server sexmod thread " : "client sexmod thread ") + id;
        Thread t = new Thread(() -> {
            try { Thread.sleep(millis); } catch (Exception e) { e.printStackTrace(); }
            task.run();
        }, name);
        t.setDaemon(true);
        t.start();
    }

    private static boolean isServerThread() {
        return Thread.currentThread().getName().contains("Server");
    }
}
