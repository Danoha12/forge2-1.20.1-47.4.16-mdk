package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import software.bernie.geckolib.model.GeoModel;

/**
 * GoblinHandRenderer — Portado a 1.20.1.
 * * Subclase de NpcArmRenderer específica para el Goblin.
 * * Escala (0.8x) y reposiciona (-1.25y) el modelo de los brazos para
 * que coincida con la estatura reducida de la entidad.
 */
public class GoblinHandRenderer extends NpcArmRenderer<BaseNpcEntity> {

    public GoblinHandRenderer(GeoModel<BaseNpcEntity> model) {
        super(model);
    }

    // ── Transformaciones Base del Mundo ───────────────────────────────────────

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        // Baja los brazos para alinearlos con los hombros del modelo (más bajito)
        poseStack.translate(0.0, -1.25, 0.0);
        // Escala global al 80% para manos más pequeñas
        poseStack.scale(0.8F, 0.8F, 0.8F);
    }

    // ── Transformaciones en Tercera Persona ───────────────────────────────────

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);

        if (!isRightHand && !isOffHand) {
            // Ajuste fino para la mano izquierda cuando no tiene nada en la mano secundaria
            poseStack.translate(0.0, -0.1, 0.05);
            poseStack.mulPose(Axis.XP.rotationDegrees(40.0F));
        } else if (isRightHand && !isOffHand) {
            // Ajuste fino para la mano derecha vacía
            poseStack.translate(-0.025, -0.1, 0.0);
        }
    }

    // ── Transformaciones en Primera Persona ───────────────────────────────────

    @Override
    protected void applyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyHandTransform(poseStack, isRightHand);

        // Empuja la mano derecha un poco más hacia el centro de la pantalla
        if (isRightHand) {
            poseStack.translate(0.15, 0.0, 0.0);
        }
    }
}