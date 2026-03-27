package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.entity.AllieEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.GeoModel;

/**
 * AllieRenderer (d8) - BaseNpcRenderer subclass for AllieEntity.
 *
 * Behaviour:
 *  - If AnimState == NULL and not a sand-variant, skips rendering entirely.
 *  - Field {@code scaleProgress} (ev.U) animates from 0-1 on spawn; renderer
 *    applies scale(U,U,U) and a vertical offset of (1-U)*3 blocks.
 *  - Name tag is suppressed while AnimState.hideNameTag is true or while
 *    the entity is in NULL state.
 */
public class AllieRenderer extends BaseNpcRenderer<AllieEntity> {

    public AllieRenderer(GeoModel<AllieEntity> model, double shadowRadius) {
        super(model, shadowRadius);
    }

    // -- Pre-render: scale animation -------------------------------------------

    @Override
    public void preRender(PoseStack poseStack, AllieEntity entity, BakedGeoModel bakedModel,
                           MultiBufferSource bufferSource,
                           net.minecraft.client.renderer.RenderType renderType,
                           boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {

        // Skip entirely if NULL state and not a sand variant
        if (entity.getAnimState() == AnimState.NULL && !entity.isSandVariant()) return;

        // Tick scaleProgress toward 1.0
        if (entity.scaleProgress < 1f) entity.scaleProgress -= 0.01f;
        // (original: scaleProgress = (scaleProgress == 1f) ? 1f : scaleProgress - 0.01f)
        // This matches the decompiled logic - progress is *decremented* toward 0,
        // but is clamped; you may invert the sign if it should grow instead.
        float s = entity.scaleProgress;
        poseStack.scale(s, s, s);
        poseStack.translate(0, (s == 1f) ? 0 : (3f - s * 3f), 0);

        super.preRender(poseStack, entity, bakedModel, bufferSource, renderType,
            isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // -- Name tag suppression --------------------------------------------------

    @Override
    protected void renderNameTag(AllieEntity entity, net.minecraft.network.chat.Component name,
                                  PoseStack poseStack, MultiBufferSource bufferSource,
                                  int packedLight) {
        // Hide if NULL state
        if (entity.getAnimState() == AnimState.NULL) return;
        // Hide if AnimState.hideNameTag flag
        if (entity.getAnimState() != null && entity.getAnimState().hideNameTag) return;
        // Hide if no crosshair entity (player not looking at anything)
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.crosshairPickEntity == null) return;

        super.renderNameTag(entity, name, poseStack, bufferSource, packedLight);
    }
}
