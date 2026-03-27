package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.HashSet;

/**
 * SlimeHandRenderer (d5) - NpcArmRenderer subclass for the Slime NPC.
 *
 * Special feature: copies transforms from source bones onto mirror/overlay
 * bones so the slime mesh follows the body correctly:
 *   "slime"      - stored - "figure" (position/rotation/scale copied)
 *   "upperBody"  - stored - "dress"  (rotation copied)
 *   "head"       - stored - "hat"    (rotation copied)
 *   "boobs"      - stored - "boobsSlime" (rotation copied)
 */
public class SlimeHandRenderer extends NpcArmRenderer {

    // Cached source transforms
    private final Vector3f slimeRot   = new Vector3f();
    private final Vector3f slimeScale = new Vector3f(1,1,1);
    private final Vector3f slimePos   = new Vector3f();
    private final Vector3f upperBodyRot = new Vector3f();
    private final Vector3f torsoRot   = new Vector3f();
    private final Vector3f headRot    = new Vector3f();
    private final Vector3f boobsRot   = new Vector3f();

    public SlimeHandRenderer(software.bernie.geckolib.model.GeoModel<BaseNpcEntity> model) {
        super(model);
    }

    // -- Hidden bones (adds "figure" on top of parent set) --------------------

    @Override
    public HashSet<String> getHiddenBones() {
        HashSet<String> bones = super.getHiddenBones();
        bones.add("figure");
        return bones;
    }

    // -- Per-bone callback: capture sources, apply to mirrors -----------------

    @Override
    protected void onBoneRender(PoseStack poseStack, GeoBone bone) {
        String name = bone.getName();
        switch (name) {
            case "slime" -> {
                slimeRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
                slimeScale.set(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
                slimePos.set(bone.getPosX(), bone.getPosY(), bone.getPosZ());
            }
            case "upperBody" -> upperBodyRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            case "torso"     -> torsoRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            case "head"      -> headRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            case "boobs"     -> boobsRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());

            case "figure" -> {
                bone.setRotX(slimeRot.x); bone.setRotY(slimeRot.y); bone.setRotZ(slimeRot.z);
                bone.setScaleX(slimeScale.x); bone.setScaleY(slimeScale.y); bone.setScaleZ(slimeScale.z);
                bone.setPosX(slimePos.x); bone.setPosY(slimePos.y); bone.setPosZ(slimePos.z);
            }
            case "dress" -> {
                bone.setRotX(upperBodyRot.x); bone.setRotY(upperBodyRot.y); bone.setRotZ(upperBodyRot.z);
            }
            case "hat" -> {
                bone.setRotX(headRot.x); bone.setRotY(headRot.y); bone.setRotZ(headRot.z);
            }
            case "boobsSlime" -> {
                bone.setRotX(boobsRot.x); bone.setRotY(boobsRot.y); bone.setRotZ(boobsRot.z);
            }
        }
    }

    // -- Positioning -----------------------------------------------------------

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -1.25, 0);
        poseStack.scale(0.8f, 0.8f, 0.8f);
    }

    @Override
    protected void applyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyHandTransform(poseStack, isRightHand);
        if (isRightHand) poseStack.translate(0.15, 0, 0);
        else { poseStack.translate(-0.02, 0, 0); poseStack.mulPose(Axis.XP.rotationDegrees(90f)); }
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (isRightHand && !isOffHand) {
            poseStack.translate(-0.025, -0.025, 0);
        } else if (!isRightHand && isOffHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(120f));
        } else if (!isRightHand) {
            poseStack.translate(0, 0.4, -0.1);
            poseStack.mulPose(Axis.XP.rotationDegrees(-30f));
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
