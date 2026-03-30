package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.EllieEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

/**
 * EllieNpcRenderer — Portado a 1.20.1 / GeckoLib 4.
 * Incluye física de cabello para los huesos: backHair, frontHairL, frontHairR.
 */
public class EllieNpcRenderer extends NpcHandRenderer<EllieEntity> {

    private float headRotX = 0.0F;

    public EllieNpcRenderer(EntityRendererProvider.Context context, GeoModel<EllieEntity> model, double shadowRadius) {
        super(context, model, shadowRadius);
    }

    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        // Ellie es un poco más pequeña que un humano estándar (0.65x)
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.scale(0.65F, 0.65F, 0.65F);
    }

    // ── Transformaciones de Ítems en Mano ─────────────────────────────────────

    @Override
    protected void applyHeldItemTransform(PoseStack poseStack, boolean isMainHand, ItemStack stack) {
        super.applyHeldItemTransform(poseStack, isMainHand, stack);

        if (!stack.isEmpty()) {
            UseAnim action = stack.getUseAnimation();
            if (action == UseAnim.BOW || action == UseAnim.CROSSBOW) return;
        }

        // Rotación personalizada para que el ítem se vea natural en la mano de Ellie
        float angle = isMainHand ? 60.0F : 150.0F;
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));
        poseStack.translate(0.0, 0.08, -0.05);
    }

    // ── Física del Cabello ───────────────────────────────────────────────────

    @Override
    protected void onBoneProcess(String boneName, CoreGeoBone bone) {
        // Solo aplicar física en tercera persona para optimizar
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;

        boolean isInteractive = getEntityData().get(BaseNpcEntity.IS_INTERACTIVE);

        switch (boneName) {
            case "head" -> headRotX = bone.getRotX();

            case "backHair" -> {
                // Si Ellie inclina la cabeza hacia adelante, el pelo de atrás se levanta
                if (!isInteractive && headRotX > 0.0F) {
                    float progress = headRotX / (float)Math.toRadians(45.0);
                    float offset = Mth.clamp(progress, 0.0F, 0.75F);
                    bone.setPosZ(offset);
                    bone.setPosY(offset);
                    bone.setRotX(-headRotX);
                }
            }

            case "frontHairL", "frontHairR" -> {
                // Los mechones frontales siempre siguen la rotación inversa del cuello para gravedad
                if (!isInteractive) {
                    bone.setRotX(-headRotX);
                }
            }
        }
    }

    // ── Resolución de Ítems Especiales ──────────────────────────────────────

    @Override
    protected ItemStack resolveHeldItem(ItemStack original) {
        if (entity == null) return original;

        AnimState state = entity.getAnimState();
        // SFW: Cambiado de BLOWJOB a SPECIAL_INTERACTION_3
        if (state == AnimState.SPECIAL_INTERACTION_3 || state == AnimState.SPECIAL_INTERACTION_3_START) {
            ItemStack interactionItem = ((NpcInventoryEntity) entity).getInteractionItem();
            entity.setItemInHand(InteractionHand.MAIN_HAND, interactionItem);
            return interactionItem;
        }
        return original;
    }
}