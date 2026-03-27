package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.KoboldEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldHandRenderer (de) - Ported from 1.12.2 to 1.20.1.
 *
 * Concrete {@link ColoredNpcArmRenderer} for the Kobold NPC.
 *
 * Behaviour:
 *  - Bone colours are resolved from the {@link EyeAndKoboldColor} enum stored in
 *    {@code KoboldEntity.BODY_COLOR} synced data, split into main/secondary sets
 *    exactly as {@link KoboldEntityRenderer} does (see {@code dj}).
 *  - Eye iris bones ("irisR", "irisL") use {@code KoboldEntity.EYE_COLOR}.
 *  - The "mouth" bone has a UV offset of -0.078125 when customization index 7 == 1
 *    (open-mouth state).
 *  - {@link #applyWorldTransforms}: shrinks the model by scaleProgress.
 *  - {@link #applyEmptyHandTransform}: inverse-scale (restore world size).
 *  - Third-person translate / rotate offsets match the original GL calls.
 *  - When holding a BOW (USE_AIM action):
 *      main hand  - extra rotX(170-)
 *      off hand   - translateX(0.1)
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code GlStateManager.func_179152_a} / {@code func_179139_a} / {@code func_179109_b}
 *     / {@code func_179114_b} - {@code PoseStack.scale} / {@code translate} / {@code mulPose}
 *   - {@code EntityDataManager.func_187225_a} - {@code entity.getEntityData().get(...)}
 *   - {@code ff.N} (BODY_COLOR) / {@code ff.K} (EYE_COLOR) - {@code KoboldEntity.BODY_COLOR}
 *     / {@code KoboldEntity.EYE_COLOR}
 *   - {@code e4.a(em)} (NpcDataKeys.getCustomizationData) - {@code NpcDataKeys.getCustomizationData(entity)}
 *   - Removed 1.12.2 {@code a(null)} exception re-throw idiom
 */
public class KoboldHandRenderer extends ColoredNpcArmRenderer<KoboldEntity> {

    public KoboldHandRenderer(GeoModel<KoboldEntity> model) {
        super(model);
    }

    // -- Bone colour -----------------------------------------------------------

    @Override
    protected Vec3i getBoneColor(String boneName) {
        EyeAndKoboldColor bodyColor = EyeAndKoboldColor.valueOf(
                entityRef.getEntityData().get(KoboldEntity.BODY_COLOR));
        EyeAndKoboldColor eyeColor  = EyeAndKoboldColor.valueOf(
                entityRef.getEntityData().get(KoboldEntity.EYE_COLOR));

        if (KoboldEntityRenderer.MAIN_COLOR_BONES.contains(boneName))
            return bodyColor.getMainColor();
        if (KoboldEntityRenderer.SECONDARY_COLOR_BONES.contains(boneName))
            return bodyColor.getSecondaryColor();
        if ("irisR".equals(boneName) || "irisL".equals(boneName))
            return eyeColor.getMainColor();
        return WHITE;
    }

    // -- Scale transforms (scaleProgress) -------------------------------------

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        applyScaleShrink(poseStack);
    }

    /** Called on the inverse pass to restore world scale. */
    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        restoreScaleShrink(poseStack);
    }

    // -- Item transforms -------------------------------------------------------

    @Override
    protected void applyItemTransform(PoseStack poseStack,
                                      boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);

        // BOW / USE_AIM action
        if (stack.getItem().getUseAnimation(stack) == UseAnim.BOW) {
            if (!isRightHand) {
                poseStack.mulPose(Axis.XP.rotationDegrees(170.0f));
            } else {
                poseStack.translate(0.1, 0, 0);
            }
            return;
        }

        // Default: rotate X
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 80.0f : 180.0f));
    }

    // -- Third-person transforms -----------------------------------------------

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);

        if (isRightHand) {
            if (isOffHand) {
                poseStack.translate(0.06, 0.0, -0.13);
                poseStack.mulPose(Axis.YP.rotationDegrees(60.0f));
                poseStack.mulPose(Axis.XP.rotationDegrees(38.0f));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
            } else {
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                poseStack.translate(0, -0.3, -0.13);
            }
        } else {
            if (isOffHand) {
                poseStack.mulPose(Axis.YP.rotationDegrees(150.0f));
                poseStack.translate(0, -0.35, 0);
            } else {
                poseStack.translate(0, -0.1, -0.083);
            }
        }
    }
}
