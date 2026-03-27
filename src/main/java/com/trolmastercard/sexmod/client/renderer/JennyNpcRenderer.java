package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.NpcInventoryEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;

/**
 * Ported from dp.java (1.12.2 - 1.20.1)
 * BaseNpcRenderer with full hair physics for Jenny-style NPC models:
 *   backHair, sideHairL, sideHairR, frontHairL, frontHairR
 * Plus offhand item rendering at the "offhand" bone position.
 *
 * Also includes weapon/enchantment transfer when NPC is in appropriate AnimState.
 *
 * Original: {@code class dp extends d_}  (generic raw type)
 */
public class JennyNpcRenderer extends BaseNpcRenderer<NpcInventoryEntity> {

    /** Head X-rotation captured for the current frame (physics seed). */
    private float headRotX = 0.0F;

    public JennyNpcRenderer(EntityRendererProvider.Context context,
                             GeoModel<NpcInventoryEntity> model,
                             double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // -- Item override: copy enchantments from inventory slot -----------------

    @Override
    protected ItemStack resolveHeldItem(ItemStack original) {
        if (entity == null) return original;
        AnimState state = entity.getAnimState();
        if (state == AnimState.BLOWJOB || state == AnimState.STARTBLOWJOB) {
            ItemStack weapon    = entity.getWeaponSlotItem();      // eb.ao
            ItemStack synced    = entity.getEntityData().get(NpcInventoryEntity.SYNCED_ITEM); // eb.az
            if (synced.isEmpty()) return weapon;

            // Copy enchantments from the synced stack onto the weapon
            Map<?, Integer> enchants = EnchantmentHelper.getEnchantments(synced);
            EnchantmentHelper.setEnchantments(enchants, weapon);
            entity.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
            return weapon;
        }
        return original;
    }

    // -- Frozen check (em.G - BaseNpcEntity.FROZEN) ---------------------------

    private boolean isFrozen() {
        return entity != null && entity.getEntityData().get(BaseNpcEntity.FROZEN);
    }

    // -- Hair physics ----------------------------------------------------------

    @Override
    protected void onBoneProcess(String boneName, CoreGeoBone bone) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        boolean frozen = isFrozen();

        switch (boneName) {
            case "head" -> headRotX = bone.getRotX();

            case "backHair" -> {
                if (!frozen && headRotX > 0.0F) {
                    double t = headRotX / ItemRenderUtil.degToRad(45.0F);
                    float f = (float) MathUtil.clamp(0.0, 0.75, t);
                    bone.setPosZ(f);
                    bone.setPosY(f);
                    bone.setRotX(-headRotX);
                }
            }

            case "sideHairR", "sideHairL" -> {
                if (!frozen && headRotX > 0.0F) {
                    double t = headRotX / ItemRenderUtil.degToRad(45.0F);
                    float f = (float) MathUtil.clamp(0.0, 1.3, t);
                    bone.setPosZ(-f);
                    bone.setPosY(f);
                }
                if (!frozen) bone.setRotX(-headRotX);
            }

            case "frontHairL", "frontHairR" -> {
                if (!frozen) bone.setRotX(-headRotX);
            }

            case "offhand" -> {
                // Render the offhand item at this bone's world position
                renderOffhandItem(bone);
            }
        }
    }

    // -- Off-hand item at bone -------------------------------------------------

    /**
     * Renders the offhand item at the "offhand" bone position.
     * Skips if scale progress < 1.0 (NPC not fully spawned).
     */
    private void renderOffhandItem(CoreGeoBone bone) {
        if (entity == null) return;

        ItemStack offhand = entity.getEntityData().get(NpcInventoryEntity.OFFHAND_ITEM); // eb.ag
        if (offhand.isEmpty()) return;

        float scaleProgress = entity.getScaleProgress(); // eb.Z
        if (scaleProgress < 1.0F) return;

        PoseStack poseStack = currentPoseStack; // captured in render()
        if (poseStack == null) return;

        poseStack.pushPose();
        // Apply the GeckoLib matrix for this bone's world position
        BoneMatrixUtil.applyBoneMatrix(poseStack, bone);
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));

        float scale = entity.getScaleAmount(); // eb.aa
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemInHandRenderer().renderItem(
                entity,
                offhand,
                net.minecraft.client.renderer.block.model.ItemTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,
                false,
                poseStack,
                currentBufferSource,
                currentPackedLight
        );

        poseStack.popPose();
    }

    // -- Render context fields (captured during render()) ---------------------

    private PoseStack currentPoseStack = null;
    private MultiBufferSource currentBufferSource = null;
    private int currentPackedLight = 0;

    @Override
    public void render(NpcInventoryEntity entity,
                       float entityYaw,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight) {
        this.entity            = entity;
        this.currentPoseStack  = poseStack;
        this.currentBufferSource = bufferSource;
        this.currentPackedLight = packedLight;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
