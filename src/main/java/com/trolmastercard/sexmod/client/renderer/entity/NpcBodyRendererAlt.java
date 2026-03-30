package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;

/**
 * NpcBodyRendererAlt — Portado a 1.20.1.
 * * Variante de renderizado para NPCs más altos o con estructuras óseas distintas.
 * * Aplica traslaciones y rotaciones específicas para objetos en mano y tercera persona.
 */
@OnlyIn(Dist.CLIENT)
public class NpcBodyRendererAlt<T extends BaseNpcEntity> extends NpcArmRenderer<T> {

    public NpcBodyRendererAlt(GeoModel<T> model) {
        super(model);
    }

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        // Baja el modelo 1.5 unidades para compensar la altura del modelo custom
        poseStack.translate(0, -1.5, 0);
    }

    @Override
    protected void applyItemTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);

        UseAnim anim = stack.getItem().getUseAnimation(stack);
        // Si es un arco o bloqueo, usamos las transformaciones estándar de la clase base
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        // Rotación de "presentación" del objeto según la mano
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 90.0f : 180.0f));

        if (isRightHand) {
            poseStack.translate(0, 0.239, -0.1);
        }
        poseStack.translate(0, 0.1, -0.07);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        // Ajuste de la mano vacía para que no se vea rígida
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 90.0f : 180.0f));
        if (isRightHand) {
            poseStack.translate(0.2, -0.2, 0);
        }
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        // Lógica cinemática para la vista en tercera persona
        if (isRightHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));

            if (isOffHand) {
                poseStack.mulPose(Axis.YP.rotationDegrees(-90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(-20.0f));
                poseStack.translate(0.4, 0, 0.228);
            }
        } else {
            // Mano izquierda
            poseStack.translate(0, 0.282, 0.141);

            if (isOffHand) {
                poseStack.translate(0.165, -0.45, 0);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
                poseStack.mulPose(Axis.YP.rotationDegrees(-27.0f));
            } else {
                poseStack.translate(0, 0, -0.05);
            }
        }
    }
}