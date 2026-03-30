package com.trolmastercard.sexmod.client.model; // Sugerencia de paquete para modelos

import com.trolmastercard.sexmod.ClothingOverlayEntity; // Ajusta el import si lo moviste
// import com.trolmastercard.sexmod.registry.ModelWhitelist; // Descomenta si existe
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * ClothingOverlayModel — Portado a 1.20.1 / GeckoLib 4.
 * * Delega la búsqueda de recursos a la ModelWhitelist para soportar
 * * paquetes de ropa y modelos personalizados desde el servidor.
 */
public class ClothingOverlayModel extends GeoModel<ClothingOverlayEntity> {

  // ── Recursos de Reserva (Fallback) ───────────────────────────────────────

  // Mostrados cuando el maniquí está en modo "display" o si falta el modelo real
  private static final ResourceLocation CROSS_GEO =
          new ResourceLocation("sexmod", "geo/cross.geo.json");
  private static final ResourceLocation CROSS_TEX =
          new ResourceLocation("sexmod", "textures/cross.png");

  // Animación compartida (el "rebote" heredado del slime)
  private static final ResourceLocation ANIMATION =
          new ResourceLocation("sexmod", "animations/slime/slime.animation.json");

  // ── Overrides de GeoModel ────────────────────────────────────────────────

  @Override
  public ResourceLocation getModelResource(ClothingOverlayEntity entity) {
    // Usamos displayOnly (la variable que definimos en ClothingOverlayEntity)
    if (entity.displayOnly) return CROSS_GEO;

    String modelName = entity.getModelName();
    if (modelName == null || modelName.isEmpty()) return CROSS_GEO; // Seguridad Anti-Crasheos

    // Asumiendo que tu ModelWhitelist está portado y maneja esto
    return ModelWhitelist.getGeoLocation(modelName);
  }

  @Override
  public ResourceLocation getTextureResource(ClothingOverlayEntity entity) {
    if (entity.displayOnly) return CROSS_TEX;

    String modelName = entity.getModelName();
    if (modelName == null || modelName.isEmpty()) return CROSS_TEX; // Seguridad Anti-Crasheos

    return ModelWhitelist.getTextureLocation(modelName);
  }

  @Override
  public ResourceLocation getAnimationResource(ClothingOverlayEntity entity) {
    // La animación base compartida por todos los overlays de ropa
    return ANIMATION;
  }
}