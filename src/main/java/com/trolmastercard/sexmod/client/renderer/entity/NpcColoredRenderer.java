package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.util.RenderUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NpcColoredRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Clase base abstracta que añade sobrescritura de color por hueso, selección
 * de hijos y renderizado de ítems en puntos de anclaje ("weapon", "itemRenderer").
 */
public abstract class NpcColoredRenderer<T extends BaseNpcEntity> extends BaseNpcRenderer<T> {

    protected static final int[] WHITE_RGB = {255, 255, 255};

    /** * Caché global: (boneName.hash ^ entity.UUID.hash) → RGB[3]
     * Usamos ConcurrentHashMap para prevenir ConcurrentModificationException en multijugador.
     */
    static final Map<Integer, int[]> boneColorCache = new ConcurrentHashMap<>();

    public NpcColoredRenderer(EntityRendererProvider.Context ctx, GeoModel<T> model) {
        // En 1.20.1, GeoEntityRenderer ya no pide el EntityType ni shadowRadius en el constructor
        super(ctx, model);
    }

    public static void clearColorCache() {
        boneColorCache.clear();
    }

    // ── Resolución de Color ───────────────────────────────────────────────────

    protected int[] getCachedBoneColor(T animatable, GeoBone bone) {
        String name = bone.getName();
        int key = name.hashCode() ^ animatable.getUUID().hashCode();

        int[] cached = boneColorCache.get(key);
        if (cached != null) return cached;

        int[] color = getBoneColor(name);
        int[] transformed = transformColor(color);
        boneColorCache.put(key, transformed);

        return transformed;
    }

    protected abstract int[] getBoneColor(String boneName);

    protected int[] transformColor(int[] rgb) {
        return rgb;
    }

    // ── Helpers de Selección de Huesos ────────────────────────────────────────

    protected static GeoBone selectChildBone(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        // Ordenamos por la posición Y (Pivot Y) para asegurar consistencia
        children.sort(Comparator.comparingDouble(GeoBone::getPivotY));

        GeoBone selected = null;
        for (int i = 0; i < children.size(); i++) {
            GeoBone child = children.get(i);
            if (i == index) {
                child.setHidden(false);
                selected = child;
            } else {
                child.setHidden(true);
            }
        }
        return selected;
    }

    protected static void showChildBone(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        if (index >= 0 && index < children.size()) {
            children.get(index).setHidden(false);
        }
    }

    // ── Renderización Recursiva ───────────────────────────────────────────────

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone,
                                  RenderType renderType, MultiBufferSource bufferSource,
                                  VertexConsumer buffer, boolean isReRender, float partialTick,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        String name = bone.getName();

        // 1. Renderizado de Ítems en Anclajes
        if ("weapon".equals(name)) {
            renderWeaponBone(poseStack, animatable, bone, bufferSource, partialTick, packedLight, packedOverlay);
        } else if ("itemRenderer".equals(name) && animatable.getAnimState() == AnimState.PAYMENT) {
            renderPaymentItemBone(poseStack, animatable, bone, bufferSource, partialTick, packedLight, packedOverlay);
        }

        // 2. Aplicar color por hueso desde la subclase
        int[] rgb = getCachedBoneColor(animatable, bone);
        float r = rgb[0] / 255.0F;
        float g = rgb[1] / 255.0F;
        float b2 = rgb[2] / 255.0F;

        // 3. Callback de procesamiento personalizado
        onBoneProcess(name, bone);

        // 4. Llamada al renderizador base de GeckoLib
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, r, g, b2, alpha);
    }

    protected void onBoneProcess(String boneName, GeoBone bone) {}

    // ── Implementación de los TODOs (Renderizado de Ítems) ────────────────────

    protected void renderWeaponBone(PoseStack poseStack, T entity, GeoBone bone,
                                    MultiBufferSource bufferSource, float partialTick,
                                    int packedLight, int packedOverlay) {

        ItemStack weapon = entity.getMainHandItem();
        if (weapon.isEmpty()) return;

        poseStack.pushPose();
        // GeckoLib 4: Alinea matemáticamente el PoseStack al espacio 3D del hueso actual
        RenderUtils.prepMatrixForBone(poseStack, bone);

        // Ajuste fino (puedes modificar estos valores si el arma atraviesa la mano)
        poseStack.translate(0.0, -0.1, 0.0);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                weapon,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, // 1.20.1 usa ItemDisplayContext en lugar de TransformType
                packedLight, packedOverlay, poseStack, bufferSource, entity.level(), entity.getId()
        );

        poseStack.popPose();
    }

    protected void renderPaymentItemBone(PoseStack poseStack, T entity, GeoBone bone,
                                         MultiBufferSource bufferSource, float partialTick,
                                         int packedLight, int packedOverlay) {

        // Ejemplo: Renderizar una esmeralda o diamante durante la animación PAYMENT
        ItemStack paymentItem = new ItemStack(Items.EMERALD); // Cámbialo por el ítem real de pago

        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);

        poseStack.translate(0.0, 0.1, 0.0); // Ajuste para que se vea sobre la palma

        Minecraft.getInstance().getItemRenderer().renderStatic(
                paymentItem,
                ItemDisplayContext.GROUND,
                packedLight, packedOverlay, poseStack, bufferSource, entity.level(), entity.getId()
        );

        poseStack.popPose();
    }
}