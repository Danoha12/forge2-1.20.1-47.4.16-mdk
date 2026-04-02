package com.trolmastercard.sexmod.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemRenderUtil {

    /**
     * Renderiza un ítem en un lugar específico usando el sistema de PoseStack de la 1.20.1.
     */
    public static void renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext transformType,
                                  boolean leftHand, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        if (stack.isEmpty()) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel bakedmodel = itemRenderer.getModel(stack, entity.level(), entity, entity.getId());

        // En la 1.20.1 usamos renderStatic para que el ítem herede las luces y sombras del mundo correctamente
        itemRenderer.renderStatic(entity, stack, transformType, leftHand, poseStack, bufferSource,
                entity.level(), packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, entity.getId());
    }

    /**
     * Versión simplificada para cuando Jenny sostiene algo en una pose estática.
     */
    public static void renderItemSimple(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (stack.isEmpty()) return;

        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
    }
}