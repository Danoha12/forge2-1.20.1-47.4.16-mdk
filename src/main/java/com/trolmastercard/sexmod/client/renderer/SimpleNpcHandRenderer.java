package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.GeoModel;

/**
 * Ported from dl.java (1.12.2 - 1.20.1)
 * NpcHandRenderer subclass that shifts the NPC's render origin 1.5 units down.
 *
 * Original: {@code class dl extends dm}
 * dm = NpcHandRenderer
 *
 * The original override methods (GL translate/rotate) are translated to PoseStack
 * equivalents for 1.20.1.
 */
public class SimpleNpcHandRenderer extends NpcHandRenderer {

    public SimpleNpcHandRenderer(EntityRendererProvider.Context context,
                                  GeoModel<BaseNpcEntity> model,
                                  double shadowRadius) {
        super(context, model, shadowRadius);
    }

    /** Shifts render origin 1.5 units down the Y axis before any further rendering. */
    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        poseStack.translate(0.0F, -1.5F, 0.0F);
    }

    // -- Item rotations --------------------------------------------------------

    /**
     * Rotates the held item in main/offhand.
     * Right hand: +90- X when held, +180- X at rest.
     * Adds a Y-offset when held.
     */
    @Override
    protected void applyHeldItemTransform(PoseStack poseStack,
                                          boolean isMainHand,
                                          net.minecraft.world.item.ItemStack itemStack) {
        super.applyHeldItemTransform(poseStack, isMainHand, itemStack);

        // Skip bow/arrow actions handled by super
        if (isActionItem(itemStack)) return;

        if (isMainHand) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.0, 0.239, -0.1);
        } else {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0, 0.1, -0.07);
        }
    }

    /** Rotates the empty-hand in main/offhand. */
    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isMainHand) {
        if (isMainHand) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            poseStack.translate(0.2, -0.2, 0.0);
        } else {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            poseStack.translate(0.0, 0.0, 0.0);
        }
    }

    /**
     * Two-hand item (weapon/shield) pose.
     * Main hand: rotate 180 Y, 90 X; with offhand: additional rotation+translate.
     * Off hand:  translate + complex rotation sequence.
     */
    @Override
    protected void applyTwoHandedTransform(PoseStack poseStack,
                                            boolean isMainHand,
                                            boolean isOffHand) {
        if (isMainHand) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            if (isOffHand) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-20.0F));
                poseStack.translate(0.4, 0.0, 0.228);
            }
        } else {
            poseStack.translate(0.0, 0.282, 0.141);
            if (isOffHand) {
                poseStack.translate(0.165, -0.45, 0.0);
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-27.0F));
            } else {
                poseStack.translate(0.0, 0.0, -0.05);
            }
        }
    }

    // -- Helpers ---------------------------------------------------------------

    private boolean isActionItem(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var action = stack.getUseAnimation();
        return action == net.minecraft.world.item.UseAnim.BOW
                || action == net.minecraft.world.item.UseAnim.CROSSBOW;
    }
}
