package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.client.model.NpcHandModel;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraft.resources.ResourceLocation;

/**
 * GalathHandModel — Modelo específico para las manos de Galath.
 */
public class GalathHandModel extends NpcHandModel {
    @Override
    public ResourceLocation getTexture() {
        return new ResourceLocation(ModConstants.MOD_ID, "textures/entity/galath/hand.png");
    }
}