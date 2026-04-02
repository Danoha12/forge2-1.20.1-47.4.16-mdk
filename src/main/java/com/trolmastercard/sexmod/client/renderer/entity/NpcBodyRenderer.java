package com.trolmastercard.sexmod.client.renderer.entity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * NpcBodyRenderer — Portado a 1.20.1 (GeckoLib 4).
 * * Renderizador base para todos los NPCs del mod.
 */
public class NpcBodyRenderer<T extends BaseNpcEntity> extends GeoEntityRenderer<T> {

    // 🚨 Aquí está la bandera estática que tu KoboldShoulderRenderHandler estaba buscando
    public static boolean RENDERING_SHOULDER = false;

    public NpcBodyRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);

        // Aquí puedes añadir configuraciones por defecto, como la sombra
        this.shadowRadius = 0.4f;
    }

    // Más adelante, aquí sobreescribiremos métodos de GeckoLib para:
    // 1. Cambiar texturas dinámicamente según la ropa.
    // 2. Ocultar o mostrar huesos (hides/shows) de los modelos.
    // 3. Aplicar colores personalizados a los ojos o pelo.
}