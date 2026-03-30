package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashSet;
import java.util.Set;

/**
 * NpcArmRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Clase base para renderizadores de cuerpo/brazos.
 * * Maneja transformaciones globales, ocultamiento de huesos y ganchos para física.
 */
public abstract class NpcArmRenderer<T extends BaseNpcEntity> extends GeoEntityRenderer<T> {

    protected T entityRef;

    // ── Constructor Único (Obligatorio en 1.20.1) ────────────────────────────

    protected NpcArmRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    // ── Ciclo de Renderizado ─────────────────────────────────────────────────

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.entityRef = entity;

        poseStack.pushPose();

        // Aplicamos transformaciones globales (Escala, traslación de altura, etc.)
        applyWorldTransforms(poseStack);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    // ── Ganchos de Personalización (Sobrescribir en subclases) ────────────────

    /** Traslación/Escala global del modelo. (Antiguo c()) */
    protected void applyWorldTransforms(PoseStack poseStack) {}

    /** Ajuste de posición para ítems en mano. (Antiguo a(boolean, ItemStack)) */
    protected void applyItemTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {}

    /** Ajuste para manos vacías. (Antiguo a(boolean)) */
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {}

    /** Transformaciones específicas para tercera persona. (Antiguo a(boolean, boolean)) */
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {}

    /** Callback por cada hueso antes de dibujarse. Útil para física/IK. (Antiguo a(String, GeoBone)) */
    protected void onBoneProcess(String boneName, GeoBone bone) {}

    /** Define qué huesos no deben dibujarse. (Antiguo a()) */
    public Set<String> getHiddenBones() {
        return new HashSet<>();
    }

    // ── Procesamiento de Huesos (GeckoLib 4) ──────────────────────────────────

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String name = bone.getName();

        // 1. Ejecutar lógica de física o procesos custom
        onBoneProcess(name, bone);

        // 2. Control de visibilidad
        // En GeckoLib 4, es mejor resetear el estado de oculto antes de evaluar
        bone.setHidden(getHiddenBones().contains(name));

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}