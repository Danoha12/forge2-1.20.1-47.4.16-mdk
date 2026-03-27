package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.model.GeoModel;

import javax.vecmath.Vector4f;

/**
 * Ported from de.java (1.12.2 - 1.20.1)
 * ColoredNpcHandRenderer for Kobold NPCs.
 *
 * - Uses {@link KoboldColoredRenderer#MAIN_COLOR_BONES} and
 *   {@link KoboldColoredRenderer#SECONDARY_COLOR_BONES} for colour lookup.
 * - Applies a -0.078125 UV shift to the "mouth" bone when mouth-open flag is set.
 * - Scales DOWN before render and UP after, driven by the spawnProgress parameter.
 * - Pre/post item transforms for bow and default weapon.
 *
 * Original: {@code class de extends d9}  (d9 = ColoredNpcHandRenderer)
 */
public class KoboldNpcHandRenderer extends ColoredNpcHandRenderer {

    public KoboldNpcHandRenderer(EntityRendererProvider.Context context,
                                  GeoModel<BaseNpcEntity> model,
                                  double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // -- Colour lookup ---------------------------------------------------------

    @Override
    protected Vec3i getBoneColorByName(String boneName) {
        if (entity == null) return WHITE;
        EyeAndKoboldColor kc = EyeAndKoboldColor.valueOf(
                entity.getEntityData().get(KoboldEntity.BODY_COLOR));
        net.minecraft.core.BlockPos eyeColor = entity.getEntityData().get(KoboldEntity.EYE_COLOR);

        if (KoboldColoredRenderer.MAIN_COLOR_BONES.contains(boneName))      return kc.getMainColor();
        if (KoboldColoredRenderer.SECONDARY_COLOR_BONES.contains(boneName)) return kc.getSecondaryColor();
        if ("irisL".equals(boneName) || "irisR".equals(boneName))           return eyeColor;

        return WHITE;
    }

    // -- UV shift for "mouth" bone ---------------------------------------------

    /**
     * Returns an adjusted RGBA + UV-offset for a bone.
     * For "mouth": shifts UV by -0.078125 when the mouth-open flag (animData[7] == 1) is set.
     */
    @Override
    protected javax.vecmath.Vector4f getColorAndUvOffset(String boneName,
                                                         float r, float g, float b) {
        if ("mouth".equals(boneName)) {
            String[] animData = NpcDataKeys.getCustomizationData((BaseNpcEntity) entity);
            int mouthFlag = Integer.parseInt(animData[7]);
            if (mouthFlag == 1) {
                return new Vector4f(r, g, b, -0.078125F);
            }
        }
        return super.getColorAndUvOffset(boneName, r, g, b);
    }

    // -- Pre/post render scale (spawn shrink) ----------------------------------

    @Override
    protected void applyPreRenderScale(PoseStack poseStack) {
        float shrink = 0.25F - entity.getEntityData().get(KoboldEntity.SPAWN_PROGRESS);
        float s = 1.0F - shrink;
        poseStack.scale(s, s, s);
    }

    @Override
    protected void applyPostRenderScale(PoseStack poseStack) {
        float shrink = 0.25F - entity.getEntityData().get(KoboldEntity.SPAWN_PROGRESS);
        double inv = 1.0 / (1.0 - shrink);
        poseStack.scale((float) inv, (float) inv, (float) inv);
    }

    // -- Item transforms -------------------------------------------------------

    @Override
    protected void applyHeldItemTransform(PoseStack poseStack,
                                          boolean isMainHand,
                                          ItemStack itemStack) {
        super.applyHeldItemTransform(poseStack, isMainHand, itemStack);
        if (itemStack != null && !itemStack.isEmpty()
                && itemStack.getUseAnimation() == UseAnim.BOW) {
            // BOW special case: right hand - +170- X; left hand - +0.1 X translation
            if (!isMainHand) {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(170.0F));
            } else {
                poseStack.translate(0.1, 0.0, 0.0);
            }
            return;
        }
        // Default: right hand 80-, left hand 180-
        float angle = isMainHand ? 80.0F : 180.0F;
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
    }

    @Override
    protected void applyTwoHandedTransform(PoseStack poseStack,
                                            boolean isMainHand,
                                            boolean isOffHand) {
        super.applyTwoHandedTransform(poseStack, isMainHand, isOffHand);
        if (isMainHand) {
            if (isOffHand) {
                poseStack.translate(0.06, 0.0, -0.13);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(60.0F));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(38.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            } else {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.translate(0.0, -0.3, -0.13);
            }
        } else {
            if (isOffHand) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(150.0F));
                poseStack.translate(0.0, -0.35, 0.0);
            } else {
                poseStack.translate(0.0, -0.1, -0.083);
            }
        }
    }
}
