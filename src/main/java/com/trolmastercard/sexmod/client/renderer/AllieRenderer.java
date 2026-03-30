package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.AllieModel;
import com.trolmastercard.sexmod.entity.AllieEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * AllieRenderer — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * Subclase de BaseNpcRenderer para AllieEntity.
 *
 * Comportamiento:
 * • Animación de escala al aparecer (0 -> 1).
 * • Oculta el NameTag si el estado es NULL o si la bandera hideNameTag está activa.
 */
public class AllieRenderer extends BaseNpcRenderer<AllieEntity> {

    // En 1.20.1 es obligatorio pasar el Context al registrar el renderer
    public AllieRenderer(EntityRendererProvider.Context renderManager) {
        // Asumo que tu BaseNpcRenderer requiere el Context y el GeoModel
        super(renderManager, new AllieModel());
        this.shadowRadius = 0.5f; // Ajusta el tamaño de la sombra según necesites
    }

    // ── Pre-render: scale animation (Actualizado a GeckoLib 4) ───────────────

    @Override
    public void preRender(PoseStack poseStack, AllieEntity entity, BakedGeoModel bakedModel,
                          MultiBufferSource bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick,
                          int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {

        // Saltar renderizado si el estado es NULL y no es variante de arena
        // (Asumo que tienes un método isSandVariant() en tu AllieEntity)
        if (entity.getAnimState() == AnimState.NULL && !entity.isSandVariant()) return;

        // Corrección del Bug: Incrementar la escala hacia 1.0f (crecimiento)
        if (entity.scaleProgress < 1.0f) {
            entity.scaleProgress = Math.min(1.0f, entity.scaleProgress + 0.01f);
        }

        float s = entity.scaleProgress;
        poseStack.scale(s, s, s);

        // Compensación de altura mientras crece
        poseStack.translate(0, (s == 1.0f) ? 0 : (3.0f - s * 3.0f), 0);

        super.preRender(poseStack, entity, bakedModel, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    // ── Name tag suppression (Optimizado para 1.20.1) ─────────────────────────

    @Override
    protected boolean shouldShowName(AllieEntity entity) {
        // Ocultar si el estado es NULL
        if (entity.getAnimState() == AnimState.NULL) return false;

        // Ocultar si el AnimState tiene la bandera hideNameTag
        if (entity.getAnimState() != null && entity.getAnimState().hideNameTag) return false;

        // Ocultar si el jugador no la está mirando directamente
        Minecraft mc = Minecraft.getInstance();
        if (mc.crosshairPickEntity != entity) return false;

        return super.shouldShowName(entity);
    }
}