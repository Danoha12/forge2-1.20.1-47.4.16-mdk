package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.EllieEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.cache.object.BakedGeoModel;

public class EllieNpcRenderer extends GeoEntityRenderer<EllieEntity> {

    public EllieNpcRenderer(EntityRendererProvider.Context context, GeoModel<EllieEntity> model) {
        super(context, model);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void preRender(PoseStack poseStack, EllieEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.translate(0.0F, -0.01F, 0.0F);
        poseStack.scale(0.65F, 0.65F, 0.65F);
    }
}