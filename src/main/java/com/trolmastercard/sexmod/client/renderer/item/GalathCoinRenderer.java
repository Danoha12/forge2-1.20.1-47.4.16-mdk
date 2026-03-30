package com.trolmastercard.sexmod.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.item.GalathCoinModel;
import com.trolmastercard.sexmod.item.GalathCoinItem;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.RgbColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GalathCoinRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderizador customizado para la moneda de Galath.
 * * Dibuja el hueso "pentagram" con luz máxima (emisiva) y colores dinámicos
 * basados en el estado de invocación de la súcubo (NBT del jugador).
 */
@OnlyIn(Dist.CLIENT)
public class GalathCoinRenderer extends GeoItemRenderer<GalathCoinItem> {

  // ── Constantes de Color y Luz ─────────────────────────────────────────────

  public static final RgbColor COLOR_ACTIVE = new RgbColor(0.847F, 0.117F, 0.356F); // Hot Pink
  public static final RgbColor COLOR_INACTIVE = new RgbColor(0.447F, 0.447F, 0.447F); // Grey

  public static final int LIGHT_FULL = 240; // Brillo máximo
  public static final int LIGHT_HALF = 120; // Brillo tenue

  private static final float TRANSITION_START_MS = 1000.0F;
  private static final float TRANSITION_END_MS = 3000.0F;

  // ── Estado por Frame ──────────────────────────────────────────────────────

  private boolean isRenderingPentagram = false;
  private RgbColor currentPentagramColor = COLOR_INACTIVE;

  public GalathCoinRenderer() {
    super(new GalathCoinModel());
  }

  // ── Lógica de Renderizado (GeckoLib 4) ────────────────────────────────────

  @Override
  public void renderRecursively(PoseStack poseStack, GalathCoinItem animatable, GeoBone bone,
                                RenderType renderType, MultiBufferSource bufferSource,
                                VertexConsumer buffer, boolean isReRender, float partialTick,
                                int packedLight, int packedOverlay,
                                float red, float green, float blue, float alpha) {

    String boneName = bone.getName();

    // 1. Detección del Hueso Pentagrama
    if ("pentagram".equals(boneName)) {
      this.isRenderingPentagram = true;

      // Calcular el color y el nivel de luz emisiva
      this.currentPentagramColor = resolvePentagramColor();
      int emissiveLight = resolveEmissiveLight(partialTick);

      // Reemplazamos los colores base por los del pentagrama
      red = currentPentagramColor.r();
      green = currentPentagramColor.g();
      blue = currentPentagramColor.b();

      // Empaquetar la luz para engañar al motor de Minecraft y que brille en la oscuridad
      packedLight = net.minecraft.client.renderer.LightTexture.pack(emissiveLight, emissiveLight);

      // Requerir un buffer de tipo "Eyes" o "Translucent" para forzar la emisión de luz sin sombras
      renderType = RenderType.entityTranslucentEmissive(getTextureLocation(animatable));
      buffer = bufferSource.getBuffer(renderType);
    } else {
      this.isRenderingPentagram = false;
    }

    // 2. Dibujar el hueso usando el motor interno y altamente optimizado de GeckoLib
    super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
            isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
  }

  // ── Resolución Dinámica de Luz y Color ────────────────────────────────────

  private int resolveEmissiveLight(float partialTick) {
    if (!isHeldByLocalPlayer()) return idleLightSine(partialTick);

    long now = System.currentTimeMillis();
    CompoundTag data = Minecraft.getInstance().player.getPersistentData();
    long activateAt = data.getLong("sexmod:galath_coin_activation_time");
    long deactivateAt = data.getLong("sexmod:galath_coin_deactivation_time");

    if (activateAt != 0L) return (int) lightActivate(now, activateAt);
    if (deactivateAt != 0L) return (int) lightDeactivate(now, deactivateAt);

    // Si tienes un método estático global para saber si Galath está activa
    // if (GalathCoinItem.isGloballyActive()) return LIGHT_HALF;

    return idleLightSine(partialTick);
  }

  private RgbColor resolvePentagramColor() {
    if (!isHeldByLocalPlayer()) return COLOR_ACTIVE;

    long now = System.currentTimeMillis();
    CompoundTag data = Minecraft.getInstance().player.getPersistentData();
    long activateAt = data.getLong("sexmod:galath_coin_activation_time");
    long deactivateAt = data.getLong("sexmod:galath_coin_deactivation_time");

    if (activateAt != 0L) return colorActivate(activateAt, now);
    if (deactivateAt != 0L) return colorDeactivate(deactivateAt, now);

    // if (GalathCoinItem.isGloballyActive()) return COLOR_INACTIVE;

    return COLOR_ACTIVE;
  }

  // ── Transiciones Matemáticas ──────────────────────────────────────────────

  private float lightActivate(long now, long activatedAt) {
    float elapsed = (float) (now - activatedAt);
    if (elapsed < TRANSITION_START_MS) return LIGHT_FULL;
    if (elapsed <= TRANSITION_END_MS)
      return MathUtil.lerp(LIGHT_FULL, LIGHT_HALF, (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
    return LIGHT_HALF;
  }

  private float lightDeactivate(long now, long deactivatedAt) {
    float elapsed = (float) (now - deactivatedAt);
    if (elapsed < TRANSITION_START_MS) return LIGHT_HALF;
    if (elapsed <= TRANSITION_END_MS)
      return MathUtil.lerp(LIGHT_HALF, LIGHT_FULL, (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
    return LIGHT_FULL;
  }

  private int idleLightSine(float partialTick) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return LIGHT_HALF;
    return (int) (60.0 * Math.sin((mc.player.tickCount + partialTick) * 0.05F) + 180.0);
  }

  private RgbColor colorActivate(long activatedAt, long now) {
    float elapsed = (float) (now - activatedAt);
    if (elapsed < TRANSITION_START_MS) return COLOR_INACTIVE;
    if (elapsed <= TRANSITION_END_MS)
      return RgbColor.lerp(COLOR_INACTIVE, COLOR_ACTIVE, (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
    return COLOR_ACTIVE;
  }

  private RgbColor colorDeactivate(long deactivatedAt, long now) {
    float elapsed = (float) (now - deactivatedAt);
    if (elapsed < TRANSITION_START_MS) return COLOR_ACTIVE;
    if (elapsed <= TRANSITION_END_MS)
      return RgbColor.lerp(COLOR_ACTIVE, COLOR_INACTIVE, (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
    return COLOR_INACTIVE;
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private boolean isHeldByLocalPlayer() {
    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null) return false;
    ItemStack main = mc.player.getMainHandItem();
    ItemStack off = mc.player.getOffhandItem();
    return main.getItem() instanceof GalathCoinItem || off.getItem() instanceof GalathCoinItem;
  }
}