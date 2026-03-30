package com.trolmastercard.sexmod.client.renderer;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;

/**
 * FigureNpcRenderer — Portado a 1.20.1.
 * * Subclase de BaseNpcRenderer que oculta el hueso "figure" por defecto.
 */
public class FigureNpcRenderer<T extends BaseNpcEntity> extends BaseNpcRenderer<T> {

    // 1. Añadimos el Context y cambiamos double por float
    public FigureNpcRenderer(EntityRendererProvider.Context context, GeoModel<T> model, float shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Override
    public HashSet<String> getHiddenBones() {
        HashSet<String> bones = super.getHiddenBones();
        bones.add("figure");
        return bones;
    }
}