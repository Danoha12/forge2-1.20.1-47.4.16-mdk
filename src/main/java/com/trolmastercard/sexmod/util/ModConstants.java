package com.trolmastercard.sexmod.util;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.Random;

/**
 * ModConstants — Portado a 1.20.1.
 * * Ubicación central para constantes de texto, RNG compartido y estado global de cámara.
 */
public final class ModConstants {

  private ModConstants() {}

  // ── Metadatos del Mod ───────────────────────────────────────────────────

  public static final String MOD_ID      = "sexmod";
  public static final String MOD_NAME    = "Fapcraft";
  public static final String MOD_VERSION = "1.1.0";

  // ── Proxies (Deprecados) ────────────────────────────────────────────────

  // Nota: En 1.20.1 usamos @EventBusSubscriber en lugar de estas rutas.
  @Deprecated public static final String CLIENT_PROXY = "com.trolmastercard.sexmod.proxy.ClientProxy";
  @Deprecated public static final String COMMON_PROXY = "com.trolmastercard.sexmod.proxy.CommonProxy";

  // ── RNG Compartido ──────────────────────────────────────────────────────

  public static final Random RAND = new Random();

  // ── Constantes de Color (Hexadecimal) ───────────────────────────────────

  public static final int COLOR_KOBOLD_BODY = 0x476EFD;
  public static final int COLOR_KOBOLD_EYE  = 0x5FD15F;

  // ── Estado Global de Cámara (SOLO CLIENTE) ───────────────────────────────

  /** * ¡OJO!: Estos campos son mutables. Solo deben ser tocados desde el hilo del cliente.
   * En 1.20.1, acceder a esto desde un Servidor Dedicado causará un crash.
   */
  public static Vec3 cameraTarget   = Vec3.ZERO;
  public static Vec3 cameraPrevious = Vec3.ZERO;

  // ── Contadores de Renderizado ───────────────────────────────────────────

  public static int frameTick = 0;
  public static int guiRotationDelta = 0;

  // ── Utilidades de Lado ──────────────────────────────────────────────────

  /** Helper rápido para saber si estamos en el cliente sin usar anotaciones pesadas. */
  public static boolean isClient() {
    return FMLLoader.getDist().isClient();
  }
}