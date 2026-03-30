package com.trolmastercard.sexmod.client.renderer.entity;

import com.trolmastercard.sexmod.client.model.entity.MangleLieModel;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * MangleLieRenderer — Portado a 1.20.1.
 * * Renderizador minimalista para MangleLieEntity.
 * * Delega toda la lógica visual al BaseNpcRenderer y al MangleLieModel.
 */
public class MangleLieRenderer extends BaseNpcRenderer<MangleLieEntity> {

    public MangleLieRenderer(EntityRendererProvider.Context ctx) {
        // En 1.20.1, el constructor típico de GeckoLib 4 solo requiere
        // el contexto de renderizado y el modelo.
        // El radio de la sombra (0.3F o similar) se define usualmente en la clase base.
        super(ctx, new MangleLieModel());
    }
}