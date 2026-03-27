package com.trolmastercard.sexmod.client.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.KoboldEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * Capa para renderizar Élitros en los Kobolds.
 * Mantiene el paquete original para evitar errores de referencia.
 */
public class ElytraLayer<T extends KoboldEntity> extends GeoRenderLayer<T> {

    private static final ResourceLocation ELYTRA_TEXTURE = new ResourceLocation("textures/entity/elytra.png");
    private final ElytraModel<T> elytraModel;

    public ElytraLayer(GeoEntityRenderer<T> renderer) {
        super(renderer);
        this.elytraModel = new ElytraModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void render(PoseStack poseStack, T entity, BakedGeoModel bakedModel,
                       RenderType renderType, MultiBufferSource bufferSource,
                       net.minecraft.client.renderer.entity.EntityRenderDispatcher dispatcher,
                       net.minecraft.client.Camera camera, float partialTick) {

        // Verifica si tiene los élitros en la mochila
        if (!hasElytra(entity)) return;

        poseStack.pushPose();

        // Ajuste de posición en la espalda (puedes mover el 0.125D si se ve muy separado)
        poseStack.translate(0.0D, 0.0D, 0.125D);

        this.elytraModel.setupAnim(entity, 0, 0, entity.tickCount + partialTick, 0, 0);

        var vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(ELYTRA_TEXTURE));

        this.elytraModel.renderToBuffer(poseStack, vertexConsumer,
                getPackedLight(entity),
                OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }

    private boolean hasElytra(T entity) {
        for (int i = 0; i < entity.inventory.getContainerSize(); i++) {
            if (entity.inventory.getItem(i).is(Items.ELYTRA)) {
                return true;
            }
        }
        return false;
    }

    private int getPackedLight(T entity) {
        return net.minecraft.client.renderer.entity.EntityRenderer.getPackedLightCoords(entity, 0);
    }
}