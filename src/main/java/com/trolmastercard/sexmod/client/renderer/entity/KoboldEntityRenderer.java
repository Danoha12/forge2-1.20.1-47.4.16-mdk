package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import com.trolmastercard.sexmod.util.NpcDataKeys;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * KoboldEntityRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja el renderizado de la entidad Kobold, incluyendo:
 * - Coloreado dinámico de huesos (Cuerpo principal vs Secundario).
 * - Cambio de ítems visuales según el estado de la animación.
 * - Etiquetas de nombre personalizadas con colores de tribu.
 */
public class KoboldEntityRenderer extends GeoEntityRenderer<KoboldEntity> {

    // ── Sets de Huesos para Coloreado ────────────────────────────────────────

    public static final Set<String> MAIN_COLOR_BONES = new HashSet<>(Arrays.asList(
            "colorSpots", "neck", "head", "snout", "midSectionR", "midSectionL",
            "innerCheekLR", "innerCheekRR", "gayL", "gayR",
            "legR", "legL", "shinL", "toesL", "kneeL", "curvesL",
            "shinR", "toesR", "kneeR", "curvesR",
            "sideL", "sideR", "hip", "torsoL", "torsoR",
            "armR", "lowerArmR", "ellbowR", "armL", "lowerArmL", "ellbowL",
            "hornUL", "hornUR", "tail", "tail2", "tail3", "tail4", "tail5",
            "hornDL2", "hornDR2", "hornDR3M", "hornDL3M",
            "frecklesAL1", "frecklesAL2", "frecklesAR1", "frecklesAR2",
            "frecklesHL1", "frecklesHL2", "frecklesHR1", "frecklesHR2"
    ));

    public static final Set<String> SECONDARY_COLOR_BONES = new HashSet<>(Arrays.asList(
            "boobR", "boobL", "frontNeck", "Rside", "Lside", "frontAndInside",
            "innerCheekLL", "innerCheekRL", "layer", "layer2",
            "down", "down2", "down3", "down4", "down5", "fuckhole",
            "hornDR3S", "hornDL3S", "assholeCoverUp", "assholeCoverUp2"
    ));

    private String lastModelString = null;

    public KoboldEntityRenderer(EntityRendererProvider.Context ctx, GeoModel<KoboldEntity> model) {
        super(ctx, model);
    }

    // ── Resolución de Color por Hueso ────────────────────────────────────────

    protected Vec3i getBoneColor(KoboldEntity entity, String boneName) {
        EyeAndKoboldColor bodyColor = EyeAndKoboldColor.safeValueOf(entity.getEntityData().get(KoboldEntity.BODY_COLOR));
        EyeAndKoboldColor eyeColor = EyeAndKoboldColor.safeValueOf(entity.getEntityData().get(KoboldEntity.EYE_COLOR));

        if (MAIN_COLOR_BONES.contains(boneName)) return bodyColor.getMainColor();
        if (SECONDARY_COLOR_BONES.contains(boneName)) return bodyColor.getSecondaryColor();

        // Colorear el iris del ojo basado en el color de ojos seleccionado
        if (boneName.equals("irisR") || boneName.equals("irisL")) return eyeColor.getMainColor();

        return new Vec3i(255, 255, 255); // Blanco por defecto
    }

    // ── Inyección de Color en el Renderizado Recursivo ──────────────────────

    @Override
    public void renderRecursively(PoseStack ps, KoboldEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, com.mojang.blaze3d.vertex.VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        // Obtener el color dinámico para este hueso específico
        Vec3i color = getBoneColor(animatable, bone.getName());

        // Normalizar los valores (0.0 a 1.0) y multiplicarlos por el color base (daño/iluminación)
        float r = (color.getX() / 255.0F) * red;
        float g = (color.getY() / 255.0F) * green;
        float b = (color.getZ() / 255.0F) * blue;

        // Llamar al super con el nuevo color inyectado
        super.renderRecursively(ps, animatable, bone, renderType, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, r, g, b, alpha);
    }

    // ── Gestión de Ítems según Animación ─────────────────────────────────────

    @Override
    public ItemStack getHeldItemForBone(String boneName, KoboldEntity entity) {
        // En GeckoLib 4, este es el método correcto para mostrar ítems en huesos
        if (boneName.equals("lowerArmR")) {
            AnimState state = entity.getAnimState();
            switch (state) {
                case BOW -> {
                    boolean holdsBow = entity.getEntityData().get(KoboldEntity.HOLDS_BOW);
                    return new ItemStack(holdsBow ? Items.BOW : Items.ARROW);
                }
                case ATTACK, SHOOT -> {
                    return new ItemStack(Items.SPECTRAL_ARROW);
                }
                default -> { return entity.getMainHandItem(); }
            }
        }
        return super.getHeldItemForBone(boneName, entity);
    }

    // ── Renderizado de Etiqueta de Nombre (NameTag) ──────────────────────────

    @Override
    protected void renderNameTag(KoboldEntity entity, Component name, PoseStack ps, MultiBufferSource buffers, int light) {
        if (entity.isInteractiveMode() || entity.getAnimState().isHideNameTag()) return;

        String colorTag = entity.getEntityData().get(KoboldEntity.NAME_COLOR_TAG);
        if (colorTag == null || colorTag.equals("null")) {
            super.renderNameTag(entity, name, ps, buffers, light);
            return;
        }

        // Aplicar el color de la tribu al tag del nombre
        EyeAndKoboldColor tribeColor = EyeAndKoboldColor.safeValueOf(entity.getEntityData().get(KoboldEntity.BODY_COLOR));
        Component coloredName = Component.literal(entity.getKoboldName() + " ")
                .append(Component.literal("-" + colorTag + "-").withStyle(tribeColor.getTextStyle()));

        super.renderNameTag(entity, coloredName, ps, buffers, light);
    }
}