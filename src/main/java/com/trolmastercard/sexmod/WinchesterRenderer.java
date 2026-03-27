package com.trolmastercard.sexmod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * WinchesterRenderer (dd) - GeoItemRenderer for WinchesterItem (aj).
 *
 * In 1.12.2 this overrode renderCube to:
 *  1. Disable GL_LIGHTING when DevToolsHandler.lightDir[0] == 0
 *  2. Apply a custom Lambertian lighting pass via NpcWorldUtil.computeSexmodLightDir
 *  3. Emit vertices with the computed colour
 *
 * In GeckoLib4 / 1.20.1 the equivalent hook is overriding
 * {@link #renderCubesOfBone} to tint bones with the sexmod light direction.
 * When {@link DevToolsHandler#lightDir}[0] == 0 the custom directional light
 * is applied; otherwise the base colour is used unchanged.
 */
public class WinchesterRenderer extends GeoItemRenderer<WinchesterItem> {

    /** Up-vector constant used in the Lambertian lighting calculation. */
    private static final Vector3f UP = new Vector3f(0f, 1f, 0f);

    public WinchesterRenderer() {
        super(new WinchesterModel());
    }

    // -- Pre-render: disable lighting when sexmod light is active -------------

    @Override
    public void preRender(PoseStack poseStack, WinchesterItem item, BakedGeoModel bakedModel,
                           MultiBufferSource bufferSource, VertexConsumer buffer,
                           boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        super.preRender(poseStack, item, bakedModel, bufferSource, buffer,
            isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        // In 1.12.2: glDisable(GL_LIGHTING) when lightDir[0] == 0
        // In 1.20.1 lighting is PBR-based; we tint via colour instead (see below).
    }

    // -- Per-bone colour: apply custom sexmod directional light ----------------

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                   int packedLight, int packedOverlay,
                                   float red, float green, float blue, float alpha) {
        float litR = red, litG = green, litB = blue;

        float[] lightDir = DevToolsHandler.lightDir;
        if (lightDir != null && lightDir[0] == 0f) {
            // Compute Lambertian diffuse from the sexmod directional light
            var sexmodLight = NpcWorldUtil.getSexmodLightDir();
            if (sexmodLight != null) {
                // Approximate normal for the bone using its rotation
                Vector3f normal = new Vector3f(UP);
                // dot product: max(0, N-L) clamped to [0, 1]
                float dot = Math.max(0f,
                    normal.x * sexmodLight.x + normal.y * sexmodLight.y + normal.z * sexmodLight.z);
                float ambient = 0.3f;
                float diffuse = 0.7f * dot;
                float light = ambient + diffuse;
                litR = Math.min(1f, red   * light);
                litG = Math.min(1f, green * light);
                litB = Math.min(1f, blue  * light);
            }
        }

        super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay,
            litR, litG, litB, alpha);
    }
}
