package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.GeoModel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * JennyHandRenderer — Portado a 1.20.1.
 * * Renderizador especializado para los brazos de Jenny.
 * * Oculta huesos de anatomía interna y aplica escalas para su tamaño (0.65x).
 */
public class JennyHandRenderer<T extends BaseNpcEntity> extends NpcArmRenderer<T> {

    private static final Set<String> HIDDEN_BONES;

    static {
        Set<String> bones = new HashSet<>();
        bones.add("boobs");
        bones.add("booty");
        bones.add("vagina");
        bones.add("fuckhole");
        bones.add("leaf7");
        bones.add("leaf8");
        HIDDEN_BONES = Collections.unmodifiableSet(bones);
    }

    public JennyHandRenderer(EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    @Override
    public Set<String> getHiddenBones() {
        return HIDDEN_BONES;
    }

    // ── Transformaciones de Mundo ───────────────────────────────────────────

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        // Jenny es un 35% más pequeña que un jugador normal
        poseStack.translate(0, -1.0, -0.05);
        poseStack.scale(0.65f, 0.65f, 0.65f);
    }

    // ── Transformaciones de Mano Vacía ──────────────────────────────────────

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        // Ajuste lateral para que el brazo no estorbe la visión
        if (isRightHand) {
            poseStack.translate(0.15, 0, 0);
        }
    }

    // ── Transformaciones en Tercera Persona ──────────────────────────────────

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        // Nota: super() aquí no hace nada porque en la clase padre está vacío,
        // pero se deja por estructura.

        if (!isRightHand && !isOffHand) {
            // Brazo izquierdo relajado
            poseStack.translate(0, -0.1, 0.05);
            poseStack.mulPose(Axis.XP.rotationDegrees(40.0f));
        } else if (isRightHand && !isOffHand) {
            // Brazo derecho principal
            poseStack.translate(-0.025, -0.1, 0);
        }
    }
}