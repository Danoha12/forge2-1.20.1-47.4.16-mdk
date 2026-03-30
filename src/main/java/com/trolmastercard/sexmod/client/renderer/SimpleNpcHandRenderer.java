package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete de renderizadores

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.model.GeoModel; // Asumiendo que NpcHandRenderer maneja GeoRenderer

/**
 * SimpleNpcHandRenderer — Portado a 1.20.1.
 * * Subclase que desplaza el origen de renderizado de la mano hacia abajo
 * * y aplica transformaciones hardcodeadas para diferentes posturas de ítems.
 */
public class SimpleNpcHandRenderer extends NpcHandRenderer {

    public SimpleNpcHandRenderer(EntityRendererProvider.Context context,
                                 GeoModel<BaseNpcEntity> model,
                                 double shadowRadius) {
        super(context, model, shadowRadius);
    }

    /** * Desplaza el origen de renderizado 1.5 unidades hacia abajo en el eje Y.
     * Compensa las diferencias de pivote entre Blockbench y el motor del juego.
     */
    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        poseStack.translate(0.0F, -1.5F, 0.0F);
    }

    // ── Rotaciones de Ítems ──────────────────────────────────────────────────

    @Override
    protected void applyHeldItemTransform(PoseStack poseStack, boolean isMainHand, ItemStack itemStack) {
        super.applyHeldItemTransform(poseStack, isMainHand, itemStack);

        // Omitir acciones de arco/ballesta que se manejan en la clase padre
        if (isActionItem(itemStack)) return;

        if (isMainHand) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.0D, 0.239D, -0.1D);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0D, 0.1D, -0.07D);
        }
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isMainHand) {
        if (isMainHand) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.2D, -0.2D, 0.0D);
        } else {
            poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            // poseStack.translate(0.0D, 0.0D, 0.0D); // Redundante, omitido por eficiencia
        }
    }

    @Override
    protected void applyTwoHandedTransform(PoseStack poseStack, boolean isMainHand, boolean isOffHand) {
        if (isMainHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));

            if (isOffHand) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F));
                poseStack.translate(0.4D, 0.0D, 0.228D);
            }
        } else {
            poseStack.translate(0.0D, 0.282D, 0.141D);

            if (isOffHand) {
                poseStack.translate(0.165D, -0.45D, 0.0D);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(-27.0F));
            } else {
                poseStack.translate(0.0D, 0.0D, -0.05D);
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isActionItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        UseAnim action = stack.getUseAnimation();
        return action == UseAnim.BOW || action == UseAnim.CROSSBOW;
    }
}