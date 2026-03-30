package com.trolmastercard.sexmod.client.renderer;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * FzEntityRenderer — Portado a 1.20.1.
 * * Renderizador genérico para la entidad ofuscada "fz".
 * * TODO: Cambiar 'T' por la clase final (ej: SlimeGirlEntity) cuando descubras quién es.
 */
public class FzEntityRenderer<T extends BaseNpcEntity> extends BaseNpcRenderer<T> {

    public FzEntityRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float shadowRadius) {
        // Corrección vital: En 1.20.1 y GeckoLib 4, shadowRadius SIEMPRE es float.
        super(context, model, shadowRadius);
    }
}