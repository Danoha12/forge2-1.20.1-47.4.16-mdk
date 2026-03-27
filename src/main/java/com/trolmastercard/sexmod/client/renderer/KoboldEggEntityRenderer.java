package com.trolmastercard.sexmod.client.renderer;

import com.trolmastercard.sexmod.entity.KoboldEgg;
import com.trolmastercard.sexmod.entity.EyeAndKoboldColor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.awt.Color;

/**
 * KoboldEggEntityRenderer - ported from cp.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * GeoEntityRenderer for the {@link KoboldEgg} entity (i.class in 1.12.2).
 * Overrides bone color for two named bones:
 *   "shell"      - fixed cream color {@link #SHELL_COLOR}
 *   "colorSpots" - body-color RGB from {@link EyeAndKoboldColor#getMainColor()}
 *
 * Field mapping:
 *   b = SHELL_COLOR  (Color(223, 206, 155))
 *   a = current entity (stored for use in renderRecursively)
 *
 * GeckoLib 3 - 4 API changes:
 *   GeoEntityRenderer<T>(RenderManager, AnimatedGeoModel<T>)
 *     - GeoEntityRenderer<T>(EntityRendererProvider.Context, GeoModel<T>)
 *   render(GeoModel, T, partials..., color...)
 *     - renderByEntityWithPartialTick / super.render overriding actRender
 *   renderRecursively(BufferBuilder, GeoBone, r, g, b, a)
 *     - renderRecursively(PoseStack, T, GeoBone, VertexConsumer, partialTick, packedLight, packedOverlay, r, g, b, a)
 *   entity.func_184212_Q().func_187225_a(i.b)  (DATA: BODY_COLOR String)
 *     - entity.getEntityData().get(KoboldEgg.BODY_COLOR)
 *   vec3i.func_177958_n() / func_177956_o() / func_177952_p()
 *     - vec3i.getX() / getY() / getZ()
 */
public class KoboldEggEntityRenderer extends GeoEntityRenderer<KoboldEgg> {

    /** Fixed shell color: cream/beige (223, 206, 155). */
    public static final Color SHELL_COLOR = new Color(223, 206, 155);

    private KoboldEgg currentEntity;

    public KoboldEggEntityRenderer(EntityRendererProvider.Context context,
                                   GeoModel<KoboldEgg> model) {
        super(context, model);
    }

    // =========================================================================
    //  Pre-render hook to store current entity
    //  Original: cp.a(GeoModel, i, ...) called before super.render(...)
    // =========================================================================

    @Override
    public void preRender(com.mojang.blaze3d.vertex.PoseStack poseStack,
                          KoboldEgg entity,
                          BakedGeoModel model,
                          net.minecraft.client.renderer.MultiBufferSource bufferSource,
                          net.minecraft.client.renderer.RenderType renderType,
                          net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource2,
                          float partialTick, int packedLight) {
        this.currentEntity = entity;
        super.preRender(poseStack, entity, model, bufferSource, renderType, bufferSource2,
            partialTick, packedLight);
    }

    // =========================================================================
    //  renderRecursively - per-bone color override
    //  Original: cp.renderRecursively(BufferBuilder, GeoBone, r, g, b, a)
    // =========================================================================

    @Override
    public void renderRecursively(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                  KoboldEgg entity,
                                  GeoBone bone,
                                  net.minecraft.client.renderer.RenderType renderType,
                                  net.minecraft.client.renderer.MultiBufferSource bufferSource,
                                  net.minecraft.client.renderer.VertexConsumer buffer,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String boneName = bone.getName();

        if ("shell".equals(boneName)) {
            red   = SHELL_COLOR.getRed()   / 255.0F;
            green = SHELL_COLOR.getGreen() / 255.0F;
            blue  = SHELL_COLOR.getBlue()  / 255.0F;
        } else if ("colorSpots".equals(boneName) && currentEntity != null) {
            String colorName = currentEntity.getEntityData().get(KoboldEgg.BODY_COLOR);
            Vec3i rgb = EyeAndKoboldColor.safeValueOf(colorName).getMainColor();
            red   = rgb.getX() / 255.0F;
            green = rgb.getY() / 255.0F;
            blue  = rgb.getZ() / 255.0F;
        }

        super.renderRecursively(poseStack, entity, bone, renderType, bufferSource, buffer,
            partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
