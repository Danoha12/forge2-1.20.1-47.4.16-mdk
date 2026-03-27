package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.NpcInventoryEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;

/**
 * JennyBodyRenderer (di) - Ported from 1.12.2 to 1.20.1.
 *
 * {@link NpcArmRenderer} for Jenny NPC with hair physics.
 *
 * Behaviour:
 *  - {@link #applyWorldTransforms}: translate(0, -1, 0), scale(0.65, 0.65, 0.65).
 *  - Hair physics: when the entity is NOT frozen (em.G), hair bones track head
 *    rotation: backHair follows head rotX; frontHairL / frontHairR get negated rotX.
 *  - Item slot override for AnimState BOW/ATTACK - uses NpcInventoryEntity item slot.
 *  - Item transforms: BOW/BLOCK - skip; otherwise rotX(60- right / 150- left) + translate.
 *  - Third-person:
 *      left + offhand  - rotY(120-)
 *      left + normal   - translate(0, 0.3, -0.15) + rotX(-45-)
 *      right + normal  - translate(-0.025, -0.05, 0)
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code GlStateManager} rotate/translate - {@code PoseStack} methods
 *   - {@code EnumAction.BOW/BLOCK} - {@code UseAnim.BOW/BLOCK}
 *   - {@code GeoBone.getRotationX()} / {@code setRotationX()} unchanged in GeckoLib4
 *   - {@code em.G} (FROZEN data param) - {@code entity.isFrozen()} helper
 *   - {@code gc.c(45.0F)} (ItemRenderUtil.boneRotToDegrees) - {@code ItemRenderUtil.boneRotToDegrees(45f)}
 *   - {@code b6.b(0,0.75,d)} (MathUtil.clamp/lerp) - {@code MathUtil.clamp(0, 0.75, d)}
 *   - {@code a.b[state.ordinal()]} - switch on {@code AnimState}
 */
public class JennyBodyRenderer<T extends NpcInventoryEntity> extends NpcArmRenderer<T> {

    /** Head rotX captured in onBoneProcess for hair physics. */
    private float headRotX = 0.0f;

    public JennyBodyRenderer(GeoModel<T> model) {
        super(model);
    }

    // -- World-space transform -------------------------------------------------

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0, -1.0, 0);
        poseStack.scale(0.65f, 0.65f, 0.65f);
    }

    // -- Item slot override ----------------------------------------------------

    /**
     * For BOW and ATTACK states, return the weapon slot item from the NPC
     * inventory instead of the default held item.
     */
    @Nullable
    protected ItemStack resolveHeldItem(@Nullable ItemStack defaultItem) {
        if (entityRef == null) return defaultItem;
        AnimState state = entityRef.getAnimState();
        if (state == AnimState.BOW || state == AnimState.ATTACK) {
            ItemStack weapon = entityRef.getWeaponSlotItem();
            if (weapon != null) {
                // sync main-hand slot
                entityRef.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
                return weapon;
            }
        }
        return defaultItem;
    }

    // -- Frozen check ----------------------------------------------------------

    protected boolean isFrozen() {
        return entityRef != null && entityRef.isFrozen();
    }

    // -- Hair physics (per-bone callback) -------------------------------------

    @Override
    protected void onBoneProcess(String name, GeoBone bone) {
        // Skip in inventory/GUI rendering
        if (Minecraft.getInstance().screen != null) return;

        switch (name) {
            case "head" -> headRotX = bone.getRotationX();

            case "backHair" -> {
                if (!isFrozen() && headRotX > 0.0f) {
                    double d = headRotX / ItemRenderUtil.boneRotToDegrees(45.0f);
                    float f = (float) MathUtil.clamp(0.0, 0.75, d);
                    bone.setPosZ(f);
                    bone.setPosY(f);
                    bone.setRotX(-headRotX);
                }
            }

            case "frontHairL", "frontHairR" -> {
                if (!isFrozen()) {
                    bone.setRotX(-headRotX);
                }
            }
        }
    }

    // -- Item transforms -------------------------------------------------------

    @Override
    protected void applyItemTransform(PoseStack poseStack,
                                      boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);
        // BOW / BLOCK - skip extra rotation
        UseAnim anim = stack.getItem().getUseAnimation(stack);
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        poseStack.translate(0, 0.08, -0.05);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        if (isRightHand) {
            poseStack.translate(0.12, 0, 0);
        }
    }

    // -- Third-person transforms -----------------------------------------------

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);

        if (!isRightHand && isOffHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(120.0f));
            return;
        }
        if (!isRightHand) {
            poseStack.translate(0, 0.3, -0.15);
            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0f));
            return;
        }
        if (!isOffHand) {
            poseStack.translate(-0.025, -0.05, 0);
        }
    }
}
