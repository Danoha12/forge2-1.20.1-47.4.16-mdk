package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.model.GeoModel;

/**
 * StaffHandRenderer — Portado a 1.20.1.
 * * Renderizador especializado para el Bastón (Staff) de Galath.
 * * Maneja la escala reducida (0.4x) y la rotación icónica de 290 grados.
 */
public class StaffHandRenderer<T extends BaseNpcEntity> extends NpcArmRenderer<T> {

    public StaffHandRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    // ── Transformaciones Globales ───────────────────────────────────────────

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        // El bastón es un modelo pequeño pero largo, lo bajamos y escalamos
        poseStack.translate(0, -0.6, 0);
        poseStack.scale(0.4f, 0.4f, 0.4f);
    }

    // ── Transformaciones de Ítem en Mano ─────────────────────────────────────

    @Override
    protected void applyItemTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        // Rotación de "Vista" original integrada aquí
        // El ángulo de 290° es el secreto para la pose del bastón
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 290.0f : 90.0f));

        UseAnim anim = stack.getUseAnimation();
        // Si está cargando un arco o bloqueando, respetamos la pose base
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        // Ajuste de inclinación adicional según la mano (Original: applyItemInHandTransform)
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 30.0f : 135.0f));
        poseStack.translate(0, 0.05, -0.05);
    }

    // ── Transformaciones de Mano Vacía ──────────────────────────────────────

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        // Ligero desplazamiento lateral para que la mano no atraviese el cuerpo a escala 0.4
        if (isRightHand) {
            poseStack.translate(0.1, 0, 0);
        }
    }

    // ── Transformaciones de Tercera Persona / Cinemáticas ───────────────────

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        // Lógica para cuando el NPC sostiene el bastón en escenas (Original: applyFirstPersonTransform)
        if (isRightHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            poseStack.translate(0, -0.14, -0.17);

            if (isOffHand) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
                poseStack.translate(0.067, 0, 0);
            }
        } else {
            // Caso de mano secundaria (Offhand)
            if (isOffHand) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
                poseStack.translate(0, 0.165, 0);
            } else {
                // Brazo relajado (Original: applyThirdPersonTransform)
                poseStack.translate(-0.025, -0.1, 0);
            }
        }
    }
}