package com.trolmastercard.sexmod.client.renderer.entity;

import com.trolmastercard.sexmod.entity.NpcSubtypeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

/**
 * NpcSubtypeRenderer — Portado a 1.20.1.
 * * Renderer trivial para subtipos de NPCs (antiguo du.class).
 * * Hereda toda la lógica de capas, ropa e ítems de BaseNpcRenderer.
 */
public class NpcSubtypeRenderer extends BaseNpcRenderer<NpcSubtypeEntity> {

    public NpcSubtypeRenderer(EntityRendererProvider.Context context,
                              GeoModel<NpcSubtypeEntity> model,
                              float shadowRadius) {
        // En 1.20.1 el shadowRadius es float, no double.
        super(context, model, shadowRadius);
    }

    /**
     * Si necesitas que este subtipo use una escala diferente o tenga
     * un renderizado de nombre especial, podrías sobrescribir métodos aquí.
     * Por ahora, al ser un porte directo de la 'du.class', se mantiene vacío.
     */
}