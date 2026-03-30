package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.renderer.entity.layers.KoboldEggOuterLayer;
import com.trolmastercard.sexmod.entity.KoboldEgg;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * KoboldEggRenderer — Portado a 1.20.1.
 * * Renderizador para el huevo de la tribu basado en la lógica del Slime.
 * * Maneja la sombra dinámica y la animación de deformación (Squish).
 */
public class KoboldEggRenderer extends MobRenderer<KoboldEgg, SlimeModel<KoboldEgg>> {

    private static final ResourceLocation SLIME_TEXTURE =
            new ResourceLocation("textures/entity/slime/slime.png");

    public KoboldEggRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SlimeModel<>(ctx.bakeLayer(ModelLayers.SLIME)), 0.25F);
        // Capa exterior que define visualmente que es un huevo de Kobold
        this.addLayer(new KoboldEggOuterLayer(this, ctx));
    }

    @Override
    public void render(KoboldEgg egg, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        // La sombra crece junto con el huevo
        this.shadowRadius = 0.25F * (float) egg.getSize();
        super.render(egg, yaw, partialTick, poseStack, buffers, packedLight);
    }

    /**
     * Aplica la escala y la deformación de salto.
     * Basado en la fórmula clásica de la 1.12.2 (GlStateManager -> PoseStack).
     */
    @Override
    protected void scale(KoboldEgg egg, PoseStack poseStack, float partialTick) {
        // Pequeño offset para evitar Z-Fighting con el bloque de abajo
        poseStack.scale(0.999F, 0.999F, 0.999F);

        float size = (float) egg.getSize();

        // Interpolar el valor de squish entre el tick anterior y el actual
        float currentSquish = egg.squish;
        float previousSquish = egg.squishOld;

        // f3 calcula la intensidad de la deformación
        float f3 = (currentSquish + (previousSquish - currentSquish) * partialTick) / (size * 0.5F + 1.0F);
        float f4 = 1.0F / (f3 + 1.0F);

        // X y Z se expanden mientras Y se encoge (y viceversa) para simular elasticidad
        poseStack.scale(f4 * size, (1.0F / f4) * size, f4 * size);
    }

    @Override
    public ResourceLocation getTextureLocation(KoboldEgg egg) {
        return SLIME_TEXTURE;
    }
}