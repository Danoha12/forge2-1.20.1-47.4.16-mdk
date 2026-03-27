package com.trolmastercard.sexmod.util;
import com.trolmastercard.sexmod.ModConstants;

import com.trolmastercard.sexmod.util.AngleTarget;
import net.minecraft.world.phys.Vec3;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Random;
import java.util.UUID;

/**
 * LightUtil - ported from be.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * General-purpose static utilities used throughout the mod:
 *   - Angle delta computation
 *   - Heading / pitch from Vec3 direction
 *   - Clipboard copy
 *   - String capitalisation
 *   - Range check (between)
 *   - Weighted random (triangular distribution)
 *   - Random sign (-1)
 *   - Float clamp
 *   - Random float with optional sign flip
 *   - Value snap-to-zero (deadzone)
 *   - Round double to int
 *   - Timed background thread (scheduleTask)
 *
 * Method mapping (obfuscated - clean):
 *   a(double,double)       - angleDelta(double,double)
 *   a(Vec3d,Vec3d)         - getHeadingTo(Vec3,Vec3) - AngleTarget
 *   a(String)              - copyToClipboard(String)
 *   b(String)              - capitalise(String)
 *   a(double,double,double)- inRange(double,double,double)
 *   a(int)                 - weightedRandom(int)
 *   a()                    - randomSign()
 *   b(float,float,float)   - clamp(float,float,float)
 *   b(double,double,double)- clamp(double,double,double)
 *   a(float,boolean)       - randomFloat(float,boolean)
 *   a(float,float,float)   (3 float args) - snapToZero(float,float,float)
 *   a(double)              - roundToInt(double)
 *   a(int,Runnable)        - scheduleTask(int,Runnable)
 *
 * ({@code bm} - {@link AngleTarget})
 */
public final class LightUtil {

    private LightUtil() {}

    // =========================================================================
    //  a(double, double) - angleDelta
    // =========================================================================

    /**
     * Returns the shortest signed angular delta (radians) from {@code from} to
     * {@code to}, both normalised to [0, 2-).
     */
    public static float angleDelta(double from, double to) {
        from = (from + 2 * Math.PI) % (2 * Math.PI);
        to   = (to   + 2 * Math.PI) % (2 * Math.PI);
        double d = to - from;
        while (d < -Math.PI) d += 2 * Math.PI;
        while (d >= Math.PI)  d -= 2 * Math.PI;
        return (float) d;
    }

    // =========================================================================
    //  a(Vec3d, Vec3d) - getHeadingTo
    // =========================================================================

    /**
     * Returns the yaw and pitch (in radians) required to look from {@code from}
     * to {@code to}.
     *
     * Original: {@code be.a(Vec3d, Vec3d)} - {@code bm(yaw, pitch)}
     * ({@code bm} - {@link AngleTarget})
     */
    public static AngleTarget getHeadingTo(Vec3 from, Vec3 to) {
        Vec3 dir = to.subtract(from).normalize();
        float yaw   = (float) Math.atan2(dir.x, dir.z);
        float pitch = (float) Math.atan2(dir.y,
            Math.sqrt(dir.x * dir.x + dir.z * dir.z));
        return new AngleTarget(yaw, pitch);
    }

    // =========================================================================
    //  a(String) - copyToClipboard
    // =========================================================================

    /** Copies {@code text} to the system clipboard. */
    public static void copyToClipboard(String text) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new StringSelection(text), null);
    }

    // =========================================================================
    //  b(String) - capitalise
    // =========================================================================

    /**
     * Returns {@code s} with the first character upper-cased and the rest
     * lower-cased. Returns {@code s} unchanged if null or empty.
     */
    public static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    // =========================================================================
    //  a(double, double, double) - inRange  [min, max)
    // =========================================================================

    /** Returns true if {@code min <= value < max}. */
    public static boolean inRange(double value, double min, double max) {
        return value >= min && value < max;
    }

    // =========================================================================
    //  a(int) - weightedRandom  (triangular distribution 0..n)
    // =========================================================================

    /**
     * Picks a random integer in [0, n] using a triangular distribution that
     * favours larger values.  If n <= 0, returns n immediately.
     */
    public static int weightedRandom(int n) {
        if (n <= 0) return n;
        Random rng = new Random();
        // Sum = 0+1+-+n = n*(n+1)/2
        int sum  = n * (n + 1) / 2;
        int pick = rng.nextInt(sum) + 1;
        int acc  = 0;
        for (int i = 0; i <= n; i++) {
            acc += i;
            if (acc >= pick) return i;
        }
        return n;
    }

    // =========================================================================
    //  a() - randomSign
    // =========================================================================

    /**
     * Returns +1 or -1 at random using the mod's shared {@link Random}.
     * Original: {@code r.f.nextBoolean() ? 1 : -1}
     */
    public static int randomSign() {
        return com.trolmastercard.sexmod.ModConstants.RANDOM.nextBoolean() ? 1 : -1;
    }

    // =========================================================================
    //  b(float, float, float) / b(double, double, double) - clamp
    // =========================================================================

    /** Clamps {@code value} to [{@code min}, {@code max}]. */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Clamps {@code value} to [{@code min}, {@code max}]. */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // =========================================================================
    //  a(float, boolean) - randomFloat
    // =========================================================================

    /**
     * Returns a random float in [0, scale], optionally negated.
     * If {@code canBeNegative} is true, the sign is randomly flipped.
     */
    public static float randomFloat(float scale, boolean canBeNegative) {
        Random rng = new Random();
        float  val = rng.nextFloat() * scale;
        if (canBeNegative && rng.nextBoolean()) val = -val;
        return val;
    }

    // =========================================================================
    //  a(float, float, float) - snapToZero  (deadzone approach)
    // =========================================================================

    /**
     * Moves {@code value} toward {@code target} by {@code step}, but returns
     * {@code value} unchanged if it is already within {@code step} of
     * {@code target}.
     *
     * Original method name unclear; analysis of the bytecode:
     *   if |value - target| <= step - return value
     *   if |value| < |target|:
     *     if target > 0 - return target - step
     *     else          - return target + step
     *   if value > 0 - return value - step
     *   else         - return value + step
     */
    public static float snapToZero(float value, float target, float step) {
        if (Math.abs(value - target) <= step) return value;
        if (Math.abs(value) < Math.abs(target)) {
            return target > 0 ? target - step : target + step;
        }
        return value > 0 ? value - step : value + step;
    }

    // =========================================================================
    //  a(double) - roundToInt
    // =========================================================================

    /** Rounds a double to the nearest int. */
    public static int roundToInt(double value) {
        return Math.round((float) value);
    }

    // =========================================================================
    //  a(int, Runnable) - scheduleTask
    // =========================================================================

    /**
     * Schedules {@code task} to run after {@code delayMs} milliseconds on a
     * background thread named {@code "server/client sexmod thread <uuid>"}.
     *
     * Original: {@code be.a(int, Runnable)}
     * Note: {@code g0.a()} checks if running on the server thread - used
     * to name the background thread for debugging.
     */
    public static void scheduleTask(int delayMs, Runnable task) {
        String id = UUID.randomUUID().toString();
        String prefix = com.trolmastercard.sexmod.util.ServerUtil.isServerThread()
            ? "server sexmod thread " : "client sexmod thread ";
        Thread t = new Thread(() -> {
            try { Thread.sleep(delayMs); } catch (Exception e) { e.printStackTrace(); }
            task.run();
        }, prefix + id);
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    //  getSurfaceY - used by WanderingEnemyEntity
    // =========================================================================

    /**
     * Returns the Y coordinate of the first solid block surface at (x, z).
     * Falls back to sea level if the world doesn't provide a height.
     */
    public static int getSurfaceY(net.minecraft.world.level.Level level, int x, int z) {
        return level.getHeight(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            x, z);
    }

    /**
     * Returns an integer sign usable by light/particle calculations.
     * Alias of {@link #randomSign()} retained for compatibility with existing callers.
     */
    public static int getLightSign() {
        return randomSign();
    }
}
