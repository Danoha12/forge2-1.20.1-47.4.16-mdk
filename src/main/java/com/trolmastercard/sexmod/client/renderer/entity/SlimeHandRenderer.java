package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;

/**
 * SlimeHandRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Renderizador de brazos para el NPC Slime.
 * * Característica especial: Copia las transformaciones de los huesos de origen
 * hacia los huesos de espejismo/superposición para que la malla exterior
 * (slime transparente y ropa) siga al cuerpo correctamente.
 */
public class SlimeHandRenderer extends NpcArmRenderer<BaseNpcEntity> {

    // Caché de transformaciones de origen (Usando JOML para 1.20.1)
    private final Vector3f slimeRot = new Vector3f();
    private final Vector3f slimeScale = new Vector3f(1.0F, 1.0F, 1.0F);
    private final Vector3f slimePos = new Vector3f();
    private final Vector3f upperBodyRot = new Vector3f();
    private final Vector3f torsoRot = new Vector3f();
    private final Vector3f headRot = new Vector3f();
    private final Vector3f boobsRot = new Vector3f();

    public SlimeHandRenderer(GeoModel<BaseNpcEntity> model) {
        super(model);
    }

    // ── Huesos Ocultos ────────────────────────────────────────────────────────

    @Override
    public HashSet<String> getHiddenBones() {
        HashSet<String> bones = super.getHiddenBones();
        // Ocultar la figura base interna si es necesario
        bones.add("figure");
        return bones;
    }

    // ── Intercepción de Huesos (Copiar y Pegar Transformaciones) ──────────────

    @Override
    protected void onBoneRender(PoseStack poseStack, GeoBone bone) {
        String name = bone.getName();

        // 1. Capturar los estados de los huesos originales
        switch (name) {
            case "slime" -> {
                slimeRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
                slimeScale.set(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
                slimePos.set(bone.getPosX(), bone.getPosY(), bone.getPosZ());
            }
            case "upperBody" -> upperBodyRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            case "torso"     -> torsoRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            case "head"      -> headRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());
            case "boobs"     -> boobsRot.set(bone.getRotX(), bone.getRotY(), bone.getRotZ());

            // 2. Aplicar los estados capturados a las capas superpuestas (GeckoLib 4)
            case "figure" -> {
                bone.updateRotation(slimeRot.x, slimeRot.y, slimeRot.z);
                bone.updateScale(slimeScale.x, slimeScale.y, slimeScale.z);
                bone.updatePosition(slimePos.x, slimePos.y, slimePos.z);
            }
            case "dress" -> bone.updateRotation(upperBodyRot.x, upperBodyRot.y, upperBodyRot.z);
            case "hat" -> bone.updateRotation(headRot.x, headRot.y, headRot.z);
            case "boobsSlime" -> bone.updateRotation(boobsRot.x, boobsRot.y, boobsRot.z);
        }
    }

    // ── Posicionamiento del PoseStack ─────────────────────────────────────────

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        poseStack.translate(0.0, -1.25, 0.0);
        poseStack.scale(0.8F, 0.8F, 0.8F);
    }

    @Override
    protected void applyHandTransform(PoseStack poseStack, boolean isRightHand) {
        super.applyHandTransform(poseStack, isRightHand);
        if (isRightHand) {
            poseStack.translate(0.15, 0.0, 0.0);
        } else {
            poseStack.translate(-0.02, 0.0, 0.0);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);
        if (isRightHand && !isOffHand) {
            poseStack.translate(-0.025, -0.025, 0.0);
        } else if (!isRightHand && isOffHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(120.0F));
        } else if (!isRightHand) {
            poseStack.translate(0.0, 0.4, -0.1);
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F));
        }
    }

    @Override
    protected void applyItemInHandTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        super.applyItemInHandTransform(poseStack, isRightHand, stack);

        UseAnim anim = stack.getUseAnimation();
        // No alterar si está usando un arco o bloqueando con escudo/espada
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 30.0F : 135.0F));
        poseStack.translate(0.0, 0.05, -0.05);
    }
}