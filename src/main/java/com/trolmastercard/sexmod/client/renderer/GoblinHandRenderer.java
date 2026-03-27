package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * GoblinHandRenderer (db) - NpcArmRenderer subclass for the Goblin NPC.
 * Same layout as JennyHandRenderer but without the figure-related bone
 * hiding: scale(0.8), translate(0, -1.25, 0).
 */
public class GoblinHandRenderer extends NpcArmRenderer {

    public GoblinHandRenderer(software.bernie.geckolib.model.GeoModel<BaseNpcEntity> model) {
        super(model);
    }

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -1.25, 0);
        poseStack.scale(0.8f, 0.8f, 0.8f);
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (!isRightHand && !isOffHand) {
            poseStack.translate(0, -0.1, 0.05);
            poseStack.mulPose(Axis.XP.rotationDegrees(40f));
            poseStack.mulPose(Axis.YP.rotationDegrees(0f));
            poseStack.mulPose(Axis.ZP.rotationDegrees(0f));
        } else if (isRightHand && !isOffHand) {
            poseStack.translate(-0.025, -0.1, 0);
        }
    }

    @Override
    protected void applyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyHandTransform(poseStack, isRightHand);
        if (isRightHand) poseStack.translate(0.15, 0, 0);
    }
}
