package com.trolmastercard.sexmod.client.render.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.render.KoboldEggRenderer;
import com.trolmastercard.sexmod.entity.KoboldEgg;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * KoboldEggOuterLayer - ported from a4.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Renders the transparent outer shell of the kobold egg entity, using the
 * vanilla {@link SlimeModel} (size 0 = outer shell).
 *
 * In 1.12.2 this implemented {@code LayerRenderer<ay>} and used
 * {@code GlStateManager} for blending. In 1.20.1 blending is handled by
 * returning the correct {@link RenderType} from the buffer source.
 *
 * The layer is only rendered when the egg is NOT invisible
 * ({@code !entity.isInvisible()}).
 */
public class KoboldEggOuterLayer extends RenderLayer<KoboldEgg, SlimeModel<KoboldEgg>> {

    private final SlimeModel<KoboldEgg> outerModel = new SlimeModel<>(0);

    public KoboldEggOuterLayer(KoboldEggRenderer renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       KoboldEgg egg,
                       float limbSwing,
                       float limbSwingAmount,
                       float partialTick,
                       float ageInTicks,
                       float netHeadYaw,
                       float headPitch) {

        if (egg.isInvisible()) return;

        // Copy bone transforms from the parent renderer's model
        outerModel.copyPropertiesFrom(getParentModel());

        // Render with translucent blending (replaces GlStateManager blend calls)
        var vertexConsumer = bufferSource.getBuffer(
            RenderType.entityTranslucentCull(getTextureLocation(egg)));

        outerModel.renderToBuffer(
            poseStack, vertexConsumer,
            packedLight, getOverlayCoords(egg, 0.0F),
            1.0F, 1.0F, 1.0F, 1.0F);
    }
}
