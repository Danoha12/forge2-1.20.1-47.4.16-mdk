package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.client.renderer.BaseNpcRenderer;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.BoneMatrixUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * JennyNpcRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja físicas de cabello complejas (5 secciones) y renderizado de ítems en huesos.
 */
public class JennyNpcRenderer extends BaseNpcRenderer<NpcInventoryEntity> {

    private float headRotX = 0.0F;
    private PoseStack currentPoseStack;
    private MultiBufferSource currentBuffers;
    private int currentLight;

    public JennyNpcRenderer(EntityRendererProvider.Context context, GeoModel<NpcInventoryEntity> model, double shadowRadius) {
        // Al padre ya solo le mandamos las 2 cosas que nos pide
        super(context, model);
        // Y la sombra se la configuramos directamente a la variable de GeoEntityRenderer
        this.shadowRadius = (float) shadowRadius;
    }

    // ── Captura de Contexto y Lógica de Ítems ────────────────────────────────

    @Override
    public void render(NpcInventoryEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        // 1. Lógica de Ítems Especiales (Leemos tu inventario custom)
        AnimState state = entity.getAnimState();
        if (state == AnimState.SUCKBLOWJOB || state == AnimState.STARTBLOWJOB) {

            // Leemos el SLOT_MAIN_HAND de tu NpcInventoryEntity
            ItemStack weapon = entity.getEntityData().get(NpcInventoryEntity.SLOT_MAIN_HAND);

            if (weapon != null && !weapon.isEmpty()) {
                entity.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            }
        }

        // 2. Capturamos el contexto para que onBoneProcess pueda acceder a él
        this.currentPoseStack = poseStack;
        this.currentBuffers = bufferSource;
        this.currentLight = packedLight;

        // 3. ¡A dibujar se ha dicho!
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // ── Procesamiento de Huesos (Físicas y Offhand) ──────────────────────────

    @Override
    protected void onBoneProcess(String boneName, GeoBone bone) {
        NpcInventoryEntity entity = this.currentEntity;

        // Optimización: No procesar físicas en primera persona
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        boolean isInteractive = entity != null && entity.isInteractiveMode;

        switch (boneName) {
            case "head" -> headRotX = bone.getRotX();

            case "backHair" -> {
                if (!isInteractive && headRotX > 0.0F) {
                    float t = headRotX / (float) Math.toRadians(45.0f);
                    float offset = Mth.clamp(t, 0.0f, 0.75f);
                    bone.setPosZ(offset);
                    bone.setPosY(offset);
                    bone.setRotX(-headRotX);
                }
            }

            case "sideHairR", "sideHairL" -> {
                if (!isInteractive && headRotX > 0.0F) {
                    float t = headRotX / (float) Math.toRadians(45.0f);
                    float offset = Mth.clamp(t, 0.0f, 1.3f);
                    bone.setPosZ(-offset);
                    bone.setPosY(offset);
                }
                if (!isInteractive) bone.setRotX(-headRotX);
            }

            case "frontHairL", "frontHairR" -> {
                if (!isInteractive) bone.setRotX(-headRotX);
            }

            case "offhand_bone" -> { // Hueso donde se ancla el ítem secundario
                renderOffhandAtBone(bone);
            }
        }
    }

    // ── Renderizado de Ítem en Hueso ─────────────────────────────────────────

    private void renderOffhandAtBone(GeoBone bone) {
        NpcInventoryEntity entity = this.currentEntity;
        if (entity == null || currentPoseStack == null) return;

        // Usamos la variable real de tu NpcInventoryEntity
        ItemStack offhandStack = entity.getEntityData().get(NpcInventoryEntity.SLOT_OFF_HAND);
        if (offhandStack.isEmpty()) return;

        // No renderizar si el NPC está en proceso de spawn (escala < 1)
        if (entity.getScaleProgress() < 1.0F) return;

        currentPoseStack.pushPose();

        // Aplicar la matriz del hueso al PoseStack de Minecraft
        BoneMatrixUtil.applyBoneMatrix(currentPoseStack, bone);

        // Ajuste de rotación para que el ítem no quede vertical
        currentPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

        float scale = entity.getModelScale();
        currentPoseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                offhandStack,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                currentLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                currentPoseStack,
                currentBuffers,
                entity.level(),
                entity.getId()
        );

        currentPoseStack.popPose();
    }
}