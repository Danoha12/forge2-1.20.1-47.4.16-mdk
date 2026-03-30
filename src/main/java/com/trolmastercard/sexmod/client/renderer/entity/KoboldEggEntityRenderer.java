package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.KoboldEgg;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.awt.Color;

/**
 * KoboldEggEntityRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderiza el huevo de Kobold interceptando los huesos del modelo:
 * - "shell": Se pinta de color crema/beige constante.
 * - "colorSpots": Se pinta del color principal de la tribu (dinámico).
 */
public class KoboldEggEntityRenderer extends GeoEntityRenderer<KoboldEgg> {

    // Color crema base para el cascarón (223, 206, 155)
    public static final Color SHELL_COLOR = new Color(223, 206, 155);

    public KoboldEggEntityRenderer(EntityRendererProvider.Context context, GeoModel<KoboldEgg> model) {
        super(context, model);
    }

    // ── Inyección de Color por Hueso ─────────────────────────────────────────

    @Override
    public void renderRecursively(PoseStack poseStack, KoboldEgg animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String boneName = bone.getName();

        // 1. Pintar el cascarón con el color crema fijo
        if (boneName.equals("shell")) {
            red   = SHELL_COLOR.getRed()   / 255.0F;
            green = SHELL_COLOR.getGreen() / 255.0F;
            blue  = SHELL_COLOR.getBlue()  / 255.0F;
        }
        // 2. Pintar las manchas con el color de la futura tribu
        else if (boneName.equals("colorSpots")) {
            String colorName = animatable.getEntityData().get(KoboldEgg.BODY_COLOR);
            Vec3i rgb = EyeAndKoboldColor.safeValueOf(colorName).getMainColor();

            red   = rgb.getX() / 255.0F;
            green = rgb.getY() / 255.0F;
            blue  = rgb.getZ() / 255.0F;
        }

        // Llamar al super para procesar los hijos del hueso con los nuevos colores aplicados
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}