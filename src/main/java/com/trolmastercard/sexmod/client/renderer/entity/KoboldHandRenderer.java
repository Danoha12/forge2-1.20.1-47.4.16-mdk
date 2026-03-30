package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldHandRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderizador de extremidades para Kobolds con soporte de color dinámico.
 * Maneja el tinte de los huesos (cuerpo/ojos) y las transformaciones de ítems.
 */
public class KoboldHandRenderer extends ColoredNpcArmRenderer<KoboldEntity> {

    public KoboldHandRenderer(EntityRendererProvider.Context context, GeoModel<KoboldEntity> model) {
        super(context, model);
    }

    // ── Resolución de Color por Hueso ────────────────────────────────────────

    @Override
    protected Vec3i getBoneColor(String boneName) {
        // Obtenemos los colores actuales desde los DataParameters de la entidad
        String bodyColorName = entityRef.getEntityData().get(KoboldEntity.BODY_COLOR);
        String eyeColorName = entityRef.getEntityData().get(KoboldEntity.EYE_COLOR);

        EyeAndKoboldColor bodyColor = EyeAndKoboldColor.safeValueOf(bodyColorName);
        EyeAndKoboldColor eyeColor = EyeAndKoboldColor.safeValueOf(eyeColorName);

        // Clasificación de huesos según el set de colores (definido en KoboldEntityRenderer)
        if (KoboldEntityRenderer.MAIN_COLOR_BONES.contains(boneName)) {
            return bodyColor.getMainColor();
        }
        if (KoboldEntityRenderer.SECONDARY_COLOR_BONES.contains(boneName)) {
            return bodyColor.getSecondaryColor();
        }

        // Colorear el iris del ojo
        if ("irisR".equals(boneName) || "irisL".equals(boneName)) {
            return eyeColor.getMainColor();
        }

        return WHITE; // Color por defecto (1, 1, 1)
    }

    // ── Transformaciones de Escala (Spawn Progress) ──────────────────────────

    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        // Aplicamos el "shrink" basado en el progreso de escalado del NPC
        applyScaleShrink(poseStack);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        // Restauramos el tamaño para que la mano vacía no se vea minúscula
        restoreScaleShrink(poseStack);
    }

    // ── Transformaciones de Ítems (Uso y Pose) ───────────────────────────────

    @Override
    protected void applyItemTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);

        // Lógica especial para el Arco (Aiming)
        if (stack.getUseAnimation() == UseAnim.BOW) {
            if (!isRightHand) {
                // Mano izquierda: rotación extrema para apuntar
                poseStack.mulPose(Axis.XP.rotationDegrees(170.0f));
            } else {
                // Mano derecha: pequeño offset lateral
                poseStack.translate(0.1, 0, 0);
            }
            return;
        }

        // Pose por defecto para otros ítems
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 80.0f : 180.0f));
    }

    // ── Renderizado en Tercera Persona (Offsets Mágicos) ─────────────────────

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);

        if (isRightHand) {
            if (isOffHand) {
                // Posición de guardia/combate con ítem en mano secundaria
                poseStack.translate(0.06, 0.0, -0.13);
                poseStack.mulPose(Axis.YP.rotationDegrees(60.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(38.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
            } else {
                // Pose relajada derecha
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                poseStack.translate(0, -0.3, -0.13);
            }
        } else {
            if (isOffHand) {
                // Pose relajada izquierda con ítem
                poseStack.mulPose(Axis.YP.rotationDegrees(150.0f));
                poseStack.translate(0, -0.35, 0);
            } else {
                // Offset estándar mano izquierda
                poseStack.translate(0, -0.1, -0.083);
            }
        }
    }
}