package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashSet;

/**
 * JennyHandRenderer (d0) - NpcArmRenderer subclass for Jenny.
 *
 * Hides anatomical bones: boobs, booty, vagina, fuckhole, leaf7, leaf8.
 *
 * Positioning:
 *   World render: translate(0, -1, -0.05), scale(0.65, 0.65, 0.65)
 *   Main hand:    if right hand - translate(0.15, 0, 0)
 *   3rd-person L (not right, not offhand): translate(0, -0.1, 0.05) + rotX(40)
 *   3rd-person R (right, not offhand):     translate(-0.025, -0.1, 0)
 */
public class JennyHandRenderer extends NpcArmRenderer {

    public JennyHandRenderer(software.bernie.geckolib.model.GeoModel<BaseNpcEntity> model) {
        super(model);
    }

    // -- Hidden bones ----------------------------------------------------------

    @Override
    public HashSet<String> getHiddenBones() {
        return new HiddenBones();
    }

    static class HiddenBones extends HashSet<String> {
        HiddenBones() {
            add("boobs");
            add("booty");
            add("vagina");
            add("fuckhole");
            add("leaf7");
            add("leaf8");
        }
    }

    // -- Positioning callbacks (see NpcArmRenderer for contract) --------------

    /** Called once before the model is rendered (world-space scale/offset). */
    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -1, -0.05);
        poseStack.scale(0.65f, 0.65f, 0.65f);
    }

    /**
     * Called for main-hand (isRight=true) or off-hand (isRight=false) transform.
     */
    @Override
    protected void applyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyHandTransform(poseStack, isRightHand);
        if (isRightHand) poseStack.translate(0.15, 0, 0);
    }

    /**
     * Called for third-person arm rendering.
     * @param isRightHand true = main hand
     * @param isOffHand   true = off-hand slot
     */
    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (!isRightHand && !isOffHand) {
            poseStack.translate(0, -0.1, 0.05);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(40f));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(0f));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(0f));
        } else if (isRightHand && !isOffHand) {
            poseStack.translate(-0.025, -0.1, 0);
        }
    }
}
