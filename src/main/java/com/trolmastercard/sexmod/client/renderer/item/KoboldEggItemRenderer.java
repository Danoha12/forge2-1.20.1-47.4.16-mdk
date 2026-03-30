package com.trolmastercard.sexmod.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.item.KoboldEggItemModel;
import com.trolmastercard.sexmod.item.KoboldEggSpawnItem;
import com.trolmastercard.sexmod.util.KoboldColorVariant;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.awt.Color;

/**
 * KoboldEggItemRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderiza el ítem del huevo en 3D con colores dinámicos.
 * - "shell": Color crema constante definido en KoboldColorVariant.
 * - "colorSpots": Tinte basado en el índice de color del ItemStack (damage value).
 */
public class KoboldEggItemRenderer extends GeoItemRenderer<KoboldEggSpawnItem> {

    // Color crema base para el cascarón (DDF69B en hex aprox)
    public static final Color SHELL_COLOR = new Color(223, 206, 155);

    private ItemStack currentStack;

    public KoboldEggItemRenderer() {
        super(new KoboldEggItemModel());
    }

    // ── Entrada del Renderizado ──────────────────────────────────────────────

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Guardamos el stack actual para que renderRecursively sepa de qué color pintar
        this.currentStack = stack;
        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
    }

    // ── Interceptación de Huesos y Color ─────────────────────────────────────

    @Override
    public void renderRecursively(PoseStack poseStack, KoboldEggSpawnItem animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String boneName = bone.getName();

        // 1. Pintar el cascarón del huevo
        if (boneName.equals("shell")) {
            red   = SHELL_COLOR.getRed()   / 255.0F;
            green = SHELL_COLOR.getGreen() / 255.0F;
            blue  = SHELL_COLOR.getBlue()  / 255.0F;
        }
        // 2. Pintar las manchas según la variante del ítem
        else if (boneName.equals("colorSpots")) {
            KoboldColorVariant variant = getVariantFromStack(this.currentStack);
            Vec3i rgb = variant.getRgb();

            red   = rgb.getX() / 255.0F;
            green = rgb.getY() / 255.0F;
            blue  = rgb.getZ() / 255.0F;
        }

        // Llamamos al super para aplicar el color a los vértices de GeckoLib
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private KoboldColorVariant getVariantFromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return KoboldColorVariant.LIGHT_GREEN; // Default
        }
        // En 1.20.1 usamos el daño como índice de variante (simulando metadata antigua)
        int index = stack.getDamageValue();
        KoboldColorVariant[] variants = KoboldColorVariant.values();

        if (index >= 0 && index < variants.length) {
            return variants[index];
        }
        return KoboldColorVariant.LIGHT_GREEN;
    }
}