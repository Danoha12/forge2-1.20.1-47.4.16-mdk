package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ColoredNpcArmRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Extensión abstracta de NpcArmRenderer que añade soporte para colores por hueso.
 * Incluye un sistema de caché para evitar cálculos repetitivos de color en cada frame.
 */
public abstract class ColoredNpcArmRenderer<T extends BaseNpcEntity> extends NpcArmRenderer<T> {

    protected static final Vec3i WHITE = new Vec3i(255, 255, 255);

    // Caché estático: (HashCode de NombreHueso + HashCode de UUID) -> Color RGB
    private static final Map<Integer, Vec3i> BONE_COLOR_CACHE = new HashMap<>();

    public static void clearColorCache() {
        BONE_COLOR_CACHE.clear();
    }

    protected ColoredNpcArmRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    protected ColoredNpcArmRenderer(GeoModel<T> model) {
        super(model);
    }

    // ── Proveedor de Color (A implementar en KoboldHandRenderer) ─────────────

    /**
     * Devuelve el color RGB (0-255) para un hueso específico.
     */
    protected abstract Vec3i getBoneColor(String boneName);

    /**
     * Obtiene el color del caché o lo genera si no existe.
     */
    protected Vec3i getCachedBoneColor(GeoBone bone) {
        int key = bone.getName().hashCode() + (entityRef != null ? entityRef.getUUID().hashCode() : 0);

        return BONE_COLOR_CACHE.computeIfAbsent(key, k -> {
            Vec3i rawColor = getBoneColor(bone.getName());
            return applyColorTransform(rawColor);
        });
    }

    protected Vec3i applyColorTransform(Vec3i color) {
        return color; // Hook para transformaciones adicionales si se requiere
    }

    // ── Utilidades de Huesos Hijos ───────────────────────────────────────────

    /**
     * Hace visible un hueso hijo específico por su índice en la lista.
     */
    protected void showChildBone(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        if (index >= 0 && index < children.size()) {
            children.get(index).setHidden(false);
        }
    }

    /**
     * Selecciona un único hueso hijo (ordenado por Pivot Y) y oculta el resto.
     */
    protected GeoBone selectChildBone(GeoBone parent, int index) {
        List<GeoBone> children = parent.getChildBones();
        // Ordenamos para asegurar que el índice sea consistente con la jerarquía visual
        children.sort(Comparator.comparingDouble(GeoBone::getPivotY));

        GeoBone selected = null;
        for (int i = 0; i < children.size(); i++) {
            GeoBone child = children.get(i);
            boolean isSelected = (i == index);
            child.setHidden(!isSelected);
            if (isSelected) selected = child;
        }
        return selected;
    }

    // ── Lógica de Escalado Dinámico (Spawn Growth) ───────────────────────────

    protected void applyScaleShrink(PoseStack poseStack) {
        if (entityRef == null) return;
        float progress = entityRef.getScaleProgress();
        // Fórmula original: se encoge basándose en cuánto falta para el 25% de crecimiento
        float scale = 1.0f - (0.25f - progress);
        poseStack.scale(scale, scale, scale);
    }

    protected void restoreScaleShrink(PoseStack poseStack) {
        if (entityRef == null) return;
        float progress = entityRef.getScaleProgress();
        float scale = 1.0f / (1.0f - (0.25f - progress));
        poseStack.scale(scale, scale, scale);
    }

    // ── Inyección de Color en GeckoLib 4 ─────────────────────────────────────

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        // Obtener el color personalizado para este hueso
        Vec3i color = getCachedBoneColor(bone);

        // Normalizar y multiplicar por el color entrante (para respetar efectos como el rojo de daño)
        float r = red * (color.getX() / 255.0f);
        float g = green * (color.getY() / 255.0f);
        float b = blue * (color.getZ() / 255.0f);

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource,
                buffer, isReRender, partialTick, packedLight, packedOverlay,
                r, g, b, alpha);
    }
}