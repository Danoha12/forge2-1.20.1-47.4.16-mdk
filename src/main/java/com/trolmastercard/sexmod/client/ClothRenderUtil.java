package com.trolmastercard.sexmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.GeoBone;

public class ClothRenderUtil {
    /**
     * Ajusta ligeramente la escala de un hueso de ropa para que no choque con la piel.
     */
    public static void applyClothingOffset(GeoBone bone, float offset) {
        bone.setScaleX(bone.getScaleX() + offset);
        bone.setScaleY(bone.getScaleY() + offset);
        bone.setScaleZ(bone.getScaleZ() + offset);
    }
}