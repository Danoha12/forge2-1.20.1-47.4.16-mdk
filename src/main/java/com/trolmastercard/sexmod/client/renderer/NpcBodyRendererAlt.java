package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.model.GeoModel;

/**
 * NpcBodyRendererAlt (dl) - Ported from 1.12.2 to 1.20.1.
 *
 * Alternate {@link NpcArmRenderer} variant.  Translates the model down
 * 1.5 units (for taller NPCs) and has specific item + third-person
 * transforms.
 *
 * Behaviour:
 *  - {@link #applyWorldTransforms}: translate(0, -1.5, 0).
 *  - Item held: if not BOW/BLOCK, rotX(90- right / 180- left) + conditional
 *    translateY for right hand.
 *  - Empty hand: rotX(90-/180-) + conditional translateX.
 *  - Third-person:
 *      right: rotY(180-) + rotX(90-); if offhand also rotY(-90-) + rotZ(90-)
 *             + rotX(-20-) + translate(0.4, 0, 0.228)
 *      left:  translate(0, 0.282, 0.141); if offhand rotX(-90-) + rotZ(-90-)
 *             + rotY(180-) + rotY(-27-) + translate(0.165, -0.45, 0)
 *             else translate(0, 0, -0.05)
 *
 * 1.12.2 - 1.20.1 changes:
 *   - All {@code GlStateManager} calls - {@code PoseStack} methods
 *   - {@code EnumAction.BOW/BLOCK} - {@code UseAnim.BOW/BLOCK}
 */
public class NpcBodyRendererAlt<T extends BaseNpcEntity> extends NpcArmRenderer<T> {

    public NpcBodyRendererAlt(GeoModel<T> model) {
        super(model);
    }

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -1.5, 0);
    }

    @Override
    protected void applyItemTransform(PoseStack poseStack,
                                      boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);
        UseAnim anim = stack.getItem().getUseAnimation(stack);
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 90.0f : 180.0f));
        if (isRightHand) {
            poseStack.translate(0, 0.239, -0.1);
        }
        poseStack.translate(0, 0.1, -0.07);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 90.0f : 180.0f));
        if (isRightHand) {
            poseStack.translate(0.2, -0.2, 0);
        }
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
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
