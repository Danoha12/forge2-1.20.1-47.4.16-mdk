package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.layer.KoboldEggOuterLayer;
import com.trolmastercard.sexmod.entity.KoboldEgg;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * KoboldEggRenderer - ported from bp.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * {@link MobRenderer} for {@link KoboldEgg} entities.
 *
 * Uses the vanilla {@link SlimeModel} (inner body) with shadow size 0.25F,
 * scaled per {@link KoboldEgg#getSize()}, and adds a {@link KoboldEggOuterLayer}
 * on top (equivalent to {@code a4}).
 *
 * Render override:
 *   The shadow radius is set to {@code 0.25 - egg.getSize()} each frame,
 *   matching the original {@code this.field_76989_e = 0.25F * ay.h()}.
 *
 * Scale:
 *   Original: {@code GlStateManager.func_179152_a(f4 * size, 1/f4 * size, f4 * size)}
 *   where {@code f3 = (squish + (e - squish) * partialTick) / (size * 0.5 + 1)}
 *   and   {@code f4 = 1 / (f3 + 1)}.
 *   This is the classic slime squish animation.
 *
 * In 1.12.2:
 *   - Extended {@code RenderLiving<ay>} - now {@code MobRenderer<KoboldEgg, SlimeModel<KoboldEgg>>}.
 *   - {@code GlStateManager.func_179152_a} - {@code poseStack.scale(x, y, z)}.
 *   - Inner body model was {@code c_} - {@code SlimeModel} via {@code ModelLayers.SLIME}.
 *   - Layer {@code a4} - {@link KoboldEggOuterLayer}.
 *   - Texture: unchanged - {@code textures/entity/slime/slime.png}.
 */
public class KoboldEggRenderer extends MobRenderer<KoboldEgg, SlimeModel<KoboldEgg>> {

    private static final ResourceLocation SLIME_TEXTURE =
        new ResourceLocation("textures/entity/slime/slime.png");

    public KoboldEggRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,
            new SlimeModel<>(ctx.bakeLayer(ModelLayers.SLIME)),
            0.25F);
        addLayer(new KoboldEggOuterLayer(this, ctx));
    }

    @Override
    public void render(KoboldEgg egg, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        // Update shadow radius proportional to egg size
        shadowRadius = 0.25F * egg.getSize();
        super.render(egg, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    protected void scale(KoboldEgg egg, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.999F, 0.999F, 0.999F);

        float size  = egg.getSize();
        float squish = egg.squishAmount;
        float prevSquish = egg.squishFactor;   // corresponds to ay.e in 1.12.2

        float f3 = (squish + (prevSquish - squish) * partialTick) / (size * 0.5F + 1.0F);
        float f4 = 1.0F / (f3 + 1.0F);

        poseStack.scale(f4 * size, 1.0F / f4 * size, f4 * size);
    }

    @Override
    public ResourceLocation getTextureLocation(KoboldEgg egg) {
        return SLIME_TEXTURE;
    }
}
