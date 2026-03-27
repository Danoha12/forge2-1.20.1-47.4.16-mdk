package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.cache.object.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * Ported from di.java (1.12.2 - 1.20.1)
 * NpcHandRenderer subclass with hair-physics for backHair, frontHairL, frontHairR.
 * Translates head pitch into hair rotation/position offsets.
 *
 * Original: {@code class di extends dm}
 *
 * Likely the hand renderer for Ellie or a similar NPC with three-bone hair.
 */
public class EllieNpcRenderer extends NpcHandRenderer {

    /** Cached head X-rotation from the most recent frame. */
    private float headRotX = 0.0F;

    public EllieNpcRenderer(EntityRendererProvider.Context context,
                              GeoModel<BaseNpcEntity> model,
                              double shadowRadius) {
        super(context, model, shadowRadius);
    }

    /** Shift origin 1.0 unit down, scale to 0.65. */
    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.scale(0.65F, 0.65F, 0.65F);
    }

    // -- Weapon / item transforms ----------------------------------------------

    @Override
    protected void applyHeldItemTransform(PoseStack poseStack,
                                          boolean isMainHand,
                                          ItemStack itemStack) {
        super.applyHeldItemTransform(poseStack, isMainHand, itemStack);

        if (itemStack != null && !itemStack.isEmpty()) {
            var action = itemStack.getUseAnimation();
            if (action == UseAnim.BOW || action == UseAnim.CROSSBOW) return;
        }

        float angle = isMainHand ? 60.0F : 150.0F;
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
        poseStack.translate(0.0, 0.08, -0.05);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isMainHand) {
        float angle = isMainHand ? 60.0F : 150.0F;
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle));
        if (isMainHand) poseStack.translate(0.12, 0.0, 0.0);
    }

    @Override
    protected void applyTwoHandedTransform(PoseStack poseStack,
                                            boolean isMainHand,
                                            boolean isOffHand) {
        // Off-hand with offhand-slot: 120- Y rotation
        if (!isMainHand && isOffHand) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(120.0F));
            return;
        }
        // Off-hand without: translate down + -45- X
        if (!isMainHand) {
            poseStack.translate(0.0, 0.3, -0.15);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-45.0F));
            return;
        }
        // Main hand without offhand
        if (!isOffHand) {
            poseStack.translate(-0.025, -0.05, 0.0);
        }
    }

    // -- Hair physics ----------------------------------------------------------

    /**
     * Per-bone callback: tracks head pitch, then deflects backHair and frontHair
     * based on how far forward the head is pitched.
     */
    @Override
    protected void onBoneProcess(String boneName, CoreGeoBone bone) {
        // Skip in first-person view
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        boolean isFrozen = getEntityFrozenFlag();

        switch (boneName) {
            case "head" -> headRotX = bone.getRotX();

            case "backHair" -> {
                if (!isFrozen && headRotX > 0.0F) {
                    double t = headRotX / ItemRenderUtil.degToRad(45.0F);
                    float f = (float) MathUtil.clamp(0.0, 0.75, t);
                    bone.setPosZ(f);
                    bone.setPosY(f);
                    bone.setRotX(-headRotX);
                }
            }

            case "frontHairL", "frontHairR" -> {
                if (!isFrozen) {
                    bone.setRotX(-headRotX);
                }
            }
        }
    }

    // -- Helpers ---------------------------------------------------------------

    /** Returns the value of the FROZEN/isSitting DataParameter on the current entity (em.G). */
    private boolean getEntityFrozenFlag() {
        if (entity == null) return false;
        return entity.getEntityData().get(BaseNpcEntity.FROZEN);
    }

    // -- ItemStack override (use weapon from NpcInventory in appropriate AnimState) --

    @Override
    protected ItemStack resolveHeldItem(ItemStack original) {
        if (entity == null) return original;
        AnimState state = entity.getAnimState();
        if (state == AnimState.BLOWJOB || state == AnimState.STARTBLOWJOB) {
            ItemStack weapon = ((NpcInventoryEntity) entity).getWeaponSlotItem();
            entity.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
            return weapon;
        }
        return original;
    }
}
