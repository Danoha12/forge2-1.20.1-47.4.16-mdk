package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.NpcInventoryEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * NpcInventoryRenderer (dp) - Ported from 1.12.2 to 1.20.1.
 *
 * {@link BaseNpcRenderer} for {@link NpcInventoryEntity} subtypes.
 *
 * Extends the base renderer with:
 *  - Hair physics (head rotX - backHair / sideHairL/R / frontHairL/R).
 *  - Enchantment transfer from an alternate item slot to the main weapon slot.
 *  - Off-hand item rendering at the "offhand" bone (with full transform pipeline).
 *
 * Behaviour:
 *  - resolveHeldItem: for BOW/ATTACK, pull weapon from inventory slot;
 *    if there's an alternate item with enchantments, copy those enchantments.
 *  - onBoneProcess: head - store rotX; hair bones - physics based on head tilt.
 *    offhand bone - render the off-hand item stack at the bone location.
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code GlStateManager} - {@code PoseStack}
 *   - {@code EnchantmentHelper.func_82781_a/func_82782_a} -
 *     {@code EnchantmentHelper.getEnchantments/setEnchantments}
 *   - {@code ItemStack.field_190927_a} (EMPTY) - {@code ItemStack.EMPTY}
 *   - {@code p.a(MATRIX_STACK, bone)} - {@code BoneMatrixUtil.applyBoneToStack(poseStack, bone)}
 *   - {@code ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND} unchanged
 *   - {@code eb.Z} - {@code npcInventoryEntity.renderScaleFactor}
 *   - {@code eb.ag} - off-hand data parameter key {@code NpcInventoryEntity.OFF_HAND_ITEM}
 *   - {@code eb.aa} - scale field {@code NpcInventoryEntity.modelScale}
 *   - {@code gc.c(45)} - {@code ItemRenderUtil.boneRotToDegrees(45)}
 */
public class NpcInventoryRenderer<T extends NpcInventoryEntity>
        extends BaseNpcRenderer<T> {

    /** Head rotX captured per-frame for hair physics. */
    private float headRotX = 0.0f;

    public NpcInventoryRenderer(EntityRendererProvider.Context context,
                                 GeoModel<T> model,
                                 double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // -- Frozen check ----------------------------------------------------------

    protected boolean isFrozen() {
        return entityRef != null && entityRef.isFrozen();
    }

    // -- Item override ---------------------------------------------------------

    @Override
    @Nullable
    protected ItemStack resolveHeldItem(@Nullable ItemStack defaultItem) {
        if (entityRef == null) return defaultItem;
        AnimState state = entityRef.getAnimState();
        if (state == AnimState.BOW || state == AnimState.ATTACK) {
            ItemStack weapon   = entityRef.getWeaponSlotItem();
            ItemStack altStack = entityRef.getEntityData().get(NpcInventoryEntity.ALT_ITEM_SLOT);

            if (!altStack.isEmpty()) {
                // Copy enchantments from alt slot to weapon slot
                Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchants =
                        net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(altStack);
                net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(enchants, weapon);
            }

            entityRef.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
            return weapon;
        }
        return defaultItem;
    }

    // -- Hair physics per-bone -------------------------------------------------

    @Override
    protected void onBoneProcess(String name, GeoBone bone) {
        if (Minecraft.getInstance().screen != null) return; // skip in GUI

        switch (name) {
            case "head" -> headRotX = bone.getRotationX();

            case "backHair" -> {
                if (isFrozen()) break;
                double d = headRotX / ItemRenderUtil.boneRotToDegrees(45.0f);
                float f = (float) MathUtil.clamp(0.0, 0.75, d);
                bone.setPosZ(f);
                bone.setPosY(f);
                bone.setRotX(-headRotX);
            }

            case "sideHairR", "sideHairL" -> {
                if (isFrozen()) break;
                double d = headRotX / ItemRenderUtil.boneRotToDegrees(45.0f);
                float f = (float) MathUtil.clamp(0.0, 1.3, d);
                bone.setPosZ(-f);
                bone.setPosY(f);
                // fallthrough into frontHair logic below
                bone.setRotX(-headRotX);
            }

            case "frontHairL", "frontHairR" -> {
                if (!isFrozen()) bone.setRotX(-headRotX);
            }
        }
    }

    // -- Item transforms -------------------------------------------------------

    @Override
    protected void applyItemTransform(PoseStack poseStack,
                                      boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);
        net.minecraft.world.item.UseAnim anim = stack.getItem().getUseAnimation(stack);
        if (anim == net.minecraft.world.item.UseAnim.BOW ||
            anim == net.minecraft.world.item.UseAnim.BLOCK) return;

        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        poseStack.translate(0, 0.08, -0.05);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        if (isRightHand) poseStack.translate(0.12, 0, 0);
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack,
                                              boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (!isRightHand && isOffHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(120.0f));
        } else if (!isRightHand) {
            poseStack.translate(0, 0.3, -0.15);
            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0f));
        } else if (!isOffHand) {
            poseStack.translate(-0.025, -0.05, 0);
        }
    }

    // -- Off-hand item rendering at bone --------------------------------------

    /**
     * Render the off-hand inventory item at the "offhand" bone location.
     * Called from {@link BaseNpcRenderer} when the bone name is "offhand"
     * (override hook {@code renderItemAtBone}).
     */
    @Override
    protected void renderItemAtBone(PoseStack poseStack,
                                     MultiBufferSource buffers,
                                     GeoBone bone, T entity, int light) {
        ItemStack offhandItem = entity.getEntityData().get(NpcInventoryEntity.OFF_HAND_ITEM);
        if (offhandItem.isEmpty()) return;
        if (entity.getRenderScaleFactor() != 1.0f) return;

        poseStack.pushPose();
        BoneMatrixUtil.applyBoneToStack(poseStack, bone);

        float scale = entity.getModelScale();
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity, offhandItem,
                net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,
                false, poseStack, buffers, entity.level(), light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                entity.getId());

        poseStack.popPose();
    }
}
