package com.trolmastercard.sexmod.client.model; // Ajusta a tu paquete de modelos

import com.trolmastercard.sexmod.item.WinchesterItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * WinchesterModel — Portado a 1.20.1 / GeckoLib 4.
 * * GeoModel para el ítem Winchester.
 * * 🚨 OPTIMIZADO: Las rutas se almacenan en caché como constantes estáticas
 * * para evitar la asignación de memoria en cada frame del renderizador.
 */
public class WinchesterModel extends GeoModel<WinchesterItem> {

    // ── Caché de Recursos ────────────────────────────────────────────────────

    private static final ResourceLocation MODEL_RESOURCE =
            new ResourceLocation("sexmod", "geo/west/winchester.geo.json");

    private static final ResourceLocation TEXTURE_RESOURCE =
            new ResourceLocation("sexmod", "textures/items/winchester/winchester.png");

    private static final ResourceLocation ANIMATION_RESOURCE =
            new ResourceLocation("sexmod", "animations/west/winchester.animation.json");

    // ── Métodos GeoModel ─────────────────────────────────────────────────────

    @Override
    public ResourceLocation getModelResource(WinchesterItem item) {
        return MODEL_RESOURCE;
    }

    @Override
    public ResourceLocation getTextureResource(WinchesterItem item) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public ResourceLocation getAnimationResource(WinchesterItem item) {
        return ANIMATION_RESOURCE;
    }
}