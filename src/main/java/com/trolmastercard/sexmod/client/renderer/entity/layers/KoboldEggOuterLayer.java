package com.trolmastercard.sexmod.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.KoboldEgg;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * KoboldEggOuterLayer — Portado a 1.20.1.
 * * Renderiza la capa translúcida exterior del huevo.
 * * Utiliza el modelo SLIME_OUTER de vainilla para el efecto de "capa doble".
 */
public class KoboldEggOuterLayer extends RenderLayer<KoboldEgg, SlimeModel<KoboldEgg>> {

    private final SlimeModel<KoboldEgg> model;

    public KoboldEggOuterLayer(RenderLayerParent<KoboldEgg, SlimeModel<KoboldEgg>> parent, EntityRendererProvider.Context context) {
        super(parent);
        // En 1.20.1, horneamos la capa exterior específicamente para que sea un poco más grande
        this.model = new SlimeModel<>(context.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, KoboldEgg egg, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        if (egg.isInvisible()) return;

        // Sincronizar las animaciones de rebote (squish) con el modelo base
        this.getParentModel().copyPropertiesTo(this.model);
        this.model.prepareMobModel(egg, limbSwing, limbSwingAmount, partialTick);
        this.model.setupAnim(egg, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // Usamos entityTranslucent para que se vea a través del huevo
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(this.getTextureLocation(egg)));

        // Renderizamos con un tinte blanco puro (1.0f) y opacidad total (la transparencia viene del RenderType/Textura)
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight,
                LivingEntityRenderer.getOverlayCoords(egg, 0.0F),
                1.0F, 1.0F, 1.0F, 1.0F);
    }
}