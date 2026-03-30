package com.trolmastercard.sexmod.client.renderer; // Ajusta al paquete de tus renders

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext; // 🚨 El nuevo estándar en 1.20.1
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * NpcInventoryRenderer — Portado a 1.20.1.
 * * Renderizador base para NPCs con inventario.
 * * Maneja físicas de cabello, renderizado off-hand y transferencia de encantamientos.
 */
public class NpcInventoryRenderer<T extends NpcInventoryEntity> extends BaseNpcRenderer<T> {

    /** RotX de la cabeza capturado por frame para las físicas del cabello. */
    private float headRotX = 0.0f;

    public NpcInventoryRenderer(EntityRendererProvider.Context context, GeoModel<T> model, double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // ── Chequeo de Congelamiento ─────────────────────────────────────────────

    protected boolean isFrozen() {
        return this.entityRef != null && this.entityRef.isFrozen();
    }

    // ── Sobrescritura de Ítem (Con protección de FPS) ────────────────────────

    @Override
    @Nullable
    protected ItemStack resolveHeldItem(@Nullable ItemStack defaultItem) {
        if (this.entityRef == null) return defaultItem;

        AnimState state = this.entityRef.getAnimState();
        if (state == AnimState.BOW || state == AnimState.ATTACK) {
            ItemStack weapon = this.entityRef.getWeaponSlotItem();
            ItemStack altStack = this.entityRef.getEntityData().get(NpcInventoryEntity.ALT_ITEM_SLOT);

            if (!altStack.isEmpty()) {
                Map<Enchantment, Integer> altEnchants = EnchantmentHelper.getEnchantments(altStack);
                Map<Enchantment, Integer> weaponEnchants = EnchantmentHelper.getEnchantments(weapon);

                // 🛡️ ESCUDO DE FPS: Solo aplicamos si los encantamientos son diferentes.
                // Evita reescribir el NBT 60 veces por segundo.
                if (!altEnchants.equals(weaponEnchants)) {
                    EnchantmentHelper.setEnchantments(altEnchants, weapon);
                }
            }

            // Ojo: setItemInHand en el cliente solo es visual, pero igual
            // sugiero que en el futuro esto lo haga el NPC en su tick() del servidor.
            this.entityRef.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            return weapon;
        }
        return defaultItem;
    }

    // ── Físicas de Cabello por Hueso ─────────────────────────────────────────

    @Override
    protected void onBoneProcess(String name, GeoBone bone) {
        if (Minecraft.getInstance().screen != null) return; // Omitir si hay una GUI abierta

        switch (name) {
            case "head" -> this.headRotX = bone.getRotX();

            case "backHair" -> {
                if (isFrozen()) break;
                double d = this.headRotX / ItemRenderUtil.boneRotToDegrees(45.0f);
                float f = (float) MathUtil.clamp(0.0, 0.75, d);
                bone.setPosZ(f);
                bone.setPosY(f);
                bone.setRotX(-this.headRotX);
            }

            case "sideHairR", "sideHairL" -> {
                if (isFrozen()) break;
                double d = this.headRotX / ItemRenderUtil.boneRotToDegrees(45.0f);
                float f = (float) MathUtil.clamp(0.0, 1.3, d);
                bone.setPosZ(-f);
                bone.setPosY(f);
                bone.setRotX(-this.headRotX); // fallthrough replicado
            }

            case "frontHairL", "frontHairR" -> {
                if (!isFrozen()) bone.setRotX(-this.headRotX);
            }
        }
    }

    // ── Transformaciones de Ítems ────────────────────────────────────────────

    @Override
    protected void applyItemTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);
        UseAnim anim = stack.getItem().getUseAnimation(stack);

        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        poseStack.translate(0, 0.08, -0.05);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        if (isRightHand) poseStack.translate(0.12, 0, 0);
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
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

    // ── Renderizado de Ítem en Mano Secundaria (Off-hand) ────────────────────

    @Override
    protected void renderItemAtBone(PoseStack poseStack, MultiBufferSource buffers, GeoBone bone, T entity, int light) {
        ItemStack offhandItem = entity.getEntityData().get(NpcInventoryEntity.OFF_HAND_ITEM);
        if (offhandItem.isEmpty() || entity.getRenderScaleFactor() != 1.0f) return;

        poseStack.pushPose();
        BoneMatrixUtil.applyBoneToStack(poseStack, bone);

        float scale = entity.getModelScale();
        poseStack.scale(scale, scale, scale);

        // 🚨 1.20.1 usa ItemDisplayContext en lugar de TransformType
        Minecraft.getInstance().getItemRenderer().renderStatic(
                entity, offhandItem,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                false, poseStack, buffers, entity.level(), light,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                entity.getId());

        poseStack.popPose();
    }
}