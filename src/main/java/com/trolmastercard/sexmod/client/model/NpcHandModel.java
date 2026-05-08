package com.trolmastercard.sexmod.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;

/**
 * NpcHandModel — Clase base para el renderizado de manos personalizadas.
 */
public abstract class NpcHandModel {
    // Aquí podrías definir ModelParts si usas el sistema nativo de Minecraft
    public abstract ResourceLocation getTexture();
}