package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

/**
 * StaffHandRenderer (d2) - NpcArmRenderer subclass for the Staff item.
 *
 * Positioning:
 *   World-space: translate(0, -0.6, 0), scale(0.4)
 *   Main-hand view:  rotX(290-)  /  off-hand view: rotX(90-)
 *   Main hand (right):
 *     rotY(180), rotX(90), translate(0, -0.14, -0.17)
 *     + if offHand: rotZ(90), translateX(0.067)
 *   3rd-person (not right, offHand):
 *     rotX(-90), rotZ(-90), translateY(0.165)
 *   Main hand right: translateX(0.1)
 */
public class StaffHandRenderer extends NpcArmRenderer {

    public StaffHandRenderer(software.bernie.geckolib.model.GeoModel<BaseNpcEntity> model) {
        super(model);
    }

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -0.6, 0);
        poseStack.scale(0.4f, 0.4f, 0.4f);
    }

    @Override
    protected void applyViewTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 290f : 90f));
    }

    @Override
    protected void applyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyHandTransform(poseStack, isRightHand);
        if (isRightHand) poseStack.translate(0.1, 0, 0);
    }

    @Override
    protected void applyFirstPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyFirstPersonTransform(poseStack, isRightHand, isOffHand);
        if (isRightHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            poseStack.mulPose(Axis.XP.rotationDegrees(90f));
            poseStack.translate(0, -0.14, -0.17);
            if (isOffHand) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90f));
                poseStack.translate(0.067, 0, 0);
            }
        } else if (isOffHand) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-90f));
            poseStack.translate(0, 0.165, 0);
        }
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (isRightHand && !isOffHand) {
            poseStack.translate(-0.025, -0.1, 0);
        }
    }

    @Override
    protected void applyItemInHandTransform(PoseStack poseStack,
                                             boolean isRightHand, ItemStack stack) {
        super.applyItemInHandTransform(poseStack, isRightHand, stack);
        UseAnim anim = stack.getUseAnimation();
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 30f : 135f));
        poseStack.translate(0, 0.05, -0.05);
    }
}
