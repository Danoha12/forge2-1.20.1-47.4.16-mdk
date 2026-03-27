package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.ModConstants;

import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * ModConstants - ported from r.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Central location for mod-wide string constants, shared Random instance,
 * and mutable global camera/position vectors used by the sex-animation system.
 */
public final class ModConstants {

    private ModConstants() {}

    // =========================================================================
    //  Mod metadata
    // =========================================================================

    public static final String MOD_ID      = "sexmod";
    public static final String MOD_NAME    = "Fapcraft";
    public static final String MOD_VERSION = "1.1.0";

    // =========================================================================
    //  Proxy class names (kept for reference; not used in 1.20.1 Forge)
    // =========================================================================

    /** @deprecated Use sided event subscribers instead. */
    @Deprecated
    public static final String CLIENT_PROXY = "com.trolmastercard.sexmod.ClientProxy";

    /** @deprecated Use sided event subscribers instead. */
    @Deprecated
    public static final String COMMON_PROXY = "com.trolmastercard.sexmod.CommonProxy";

    // =========================================================================
    //  Shared RNG
    // =========================================================================

    /** Shared {@link Random} instance used across the mod for non-critical randomness. */
    public static final Random RANDOM = new Random();

    // =========================================================================
    //  Color constants (int-packed RGB)
    // =========================================================================

    /** Kobold body tint - pale lavender. */
    public static final int COLOR_KOBOLD_BODY = 0x476EFD;   // 4674237

    /** Kobold eye tint - soft green. */
    public static final int COLOR_KOBOLD_EYE  = 0x5FD15F;   // 6281823  (approx)

    // =========================================================================
    //  Mutable global state used by the camera / sex-animation system
    // =========================================================================

    /**
     * Current camera target position, updated each client tick during a
     * sex animation sequence.  Equivalent to {@code r.j}.
     */
    public static Vec3 cameraTarget   = Vec3.ZERO;

    /**
     * Previous camera target position (for interpolation).
     * Equivalent to {@code r.k}.
     */
    public static Vec3 cameraPrevious = Vec3.ZERO;

    // =========================================================================
    //  Misc counters (used by the rendering system)
    // =========================================================================

    /** Frame counter incremented by the client tick handler. Equivalent to {@code r.b}. */
    public static int frameTick = 0;

    /** Secondary counter used for GUI rotation delta. Equivalent to {@code r.i}. */
    public static int guiRotationDelta = 0;
}
