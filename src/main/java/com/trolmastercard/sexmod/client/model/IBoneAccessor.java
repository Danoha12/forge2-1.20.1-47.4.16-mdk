package com.trolmastercard.sexmod.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * IBoneAccessor — Portado a 1.20.1.
 * * Usada por los modelos estáticos (Manos, Huevos, Props)
 * * para exponer su hueso principal al Renderizador y aplicarles rotación dinámica.
 */
@OnlyIn(Dist.CLIENT)
public interface IBoneAccessor {

    /**
     * Aplica los ángulos de rotación en espacio local al ModelPart dado.
     */
    default void setBoneRotation(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    /**
     * Retorna el hueso principal (raíz) del modelo.
     */
    ModelPart getBoneRoot();
}