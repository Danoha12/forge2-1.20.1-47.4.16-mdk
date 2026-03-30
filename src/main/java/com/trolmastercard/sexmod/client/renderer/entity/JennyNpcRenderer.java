package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.BoneMatrixUtil;
import com.trolmastercard.sexmod.util.ItemRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

/**
 * JennyNpcRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja físicas de cabello complejas (5 secciones) y renderizado de ítems en huesos.
 * * Incluye transferencia de encantamientos visuales entre ítems de inventario.
 */
public class JennyNpcRenderer extends BaseNpcRenderer<NpcInventoryEntity> {

    private float headRotX = 0.0F;
    private PoseStack currentPoseStack;
    private MultiBufferSource currentBuffers;
    private int currentLight;

    public JennyNpcRenderer(EntityRendererProvider.Context context, GeoModel<NpcInventoryEntity> model, double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // ── Resolución de Ítems y Encantamientos ─────────────────────────────────

    @Override
    protected ItemStack resolveHeldItem(ItemStack original) {
        if (entity == null) return original;

        AnimState state = entity.getAnimState();
        // SFW: Mapeo de estados de interacción especial
        if (state == AnimState.SPECIAL_INTERACTION_A || state == AnimState.SPECIAL_INTERACTION_A_START) {
            ItemStack weapon = entity.getWeaponSlotItem();
            ItemStack synced = entity.getEntityData().get(NpcInventoryEntity.SYNCED_ITEM);

            if (synced.isEmpty()) return weapon;

            // En 1.20.1, el mapa de encantamientos usa la clase Enchantment como llave
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(synced);
            EnchantmentHelper.setEnchantments(enchants, weapon);

            entity.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            return weapon;
        }
        return original;
    }

    // ── Procesamiento de Huesos (Físicas y Offhand) ──────────────────────────

    @Override
    protected void onBoneProcess(String boneName, GeoBone bone) {
        // Optimización: No procesar físicas en primera persona
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        boolean isInteractive = entity != null && entity.isInteractiveMode();

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
        if (entity == null || currentPoseStack == null) return;

        ItemStack offhandStack = entity.getEntityData().get(NpcInventoryEntity.OFFHAND_ITEM);
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

    // ── Captura de Contexto de Renderizado ───────────────────────────────────

    @Override
    public void render(NpcInventoryEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Capturamos el contexto para que onBoneProcess pueda acceder a él
        this.currentPoseStack = poseStack;
        this.currentBuffers = bufferSource;
        this.currentLight = packedLight;

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}