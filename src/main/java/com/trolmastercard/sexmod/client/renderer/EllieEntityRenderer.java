package com.trolmastercard.sexmod.client.renderer;

import com.trolmastercard.sexmod.entity.EllieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * EllieEntityRenderer — Portado a 1.20.1.
 * * Renderizador principal para Ellie (antigua entidad "el").
 */
public class EllieEntityRenderer extends BaseNpcRenderer<EllieEntity> {

    public EllieEntityRenderer(EntityRendererProvider.Context context, GeoModel<EllieEntity> model, float shadowRadius) {
        // Nota: En la 1.20.1, el shadowRadius debe ser un float.
        super(context, model, shadowRadius);
    }
}