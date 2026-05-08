package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.ItemRenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import javax.annotation.Nullable;

/**
 * JennyBodyRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja el renderizado de extremidades e ítems para el NPC Jenny.
 * Incluye física de cabello dinámica: el pelo reacciona al movimiento de la cabeza.
 */
public class JennyBodyRenderer<T extends BaseNpcEntity> extends NpcArmRenderer<T> {
    private float headRotX = 0.0f;

    public JennyBodyRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context, GeoModel<T> model) {
        super(context, model);
    }

    // ── Transformación del Mundo (Escala NPC) ────────────────────────────────

    @Override
    protected void applyWorldTransforms(PoseStack poseStack) {
        // Jenny es más pequeña que el jugador estándar
        poseStack.translate(0, -1.0, 0);
        poseStack.scale(0.65f, 0.65f, 0.65f);
    }

    // ── Resolución de Ítems Especiales ───────────────────────────────────────

    // ── Conexión de Armas Dinámicas (GeckoLib 4) ─────────────────────────────

    @Override
    public void render(T entity, float entityYaw, float partialTick, PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight) {
        // 1. Interceptamos la señal antes de dibujar para ver en qué estado está Jenny
        com.trolmastercard.sexmod.registry.AnimState state = entity.getAnimState();

        // 2. Si está atacando o usando el arco, le forzamos el arma en la mano
        if (state == com.trolmastercard.sexmod.registry.AnimState.BOW || state == com.trolmastercard.sexmod.registry.AnimState.ATTACK) {

            // Usamos el adaptador (cast) a NpcInventoryEntity para que no marque error
            ItemStack weapon = entity.getMainHandItem();

            if (weapon != null && !weapon.isEmpty()) {
                entity.setItemInHand(InteractionHand.MAIN_HAND, weapon);
            }
        }

        // 3. Ya con el arma en la mano (si aplicaba), dejamos que el motor siga su curso
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // ── Física del Cabello (onBoneProcess) ───────────────────────────────────

    @Override
    protected void onBoneProcess(String name, GeoBone bone) {
        // No procesar físicas si estamos en un menú o inventario
        if (Minecraft.getInstance().screen != null) return;

        switch (name) {
            case "head" -> headRotX = bone.getRotX();

            case "backHair" -> {
                // El pelo de la espalda se levanta si la cabeza se inclina hacia adelante
                if (!isFrozen() && headRotX > 0.0f) {
                    float limit = (float) Math.toRadians(45.0f);
                    float progress = headRotX / limit;
                    float offset = Mth.clamp(progress, 0.0f, 0.75f);

                    bone.setPosZ(offset);
                    bone.setPosY(offset);
                    bone.setRotX(-headRotX);
                }
            }

            case "frontHairL", "frontHairR" -> {
                // Los mechones frontales cuelgan verticalmente
                if (!isFrozen()) {
                    bone.setRotX(-headRotX);
                }
            }
        }
    }

    private boolean isFrozen() {
        return entityRef != null && entityRef.isInteractiveMode;
    }

    // ── Transformaciones de Ítems ─────────────────────────────────────────────

    @Override
    protected void applyItemTransform(PoseStack poseStack, boolean isRightHand, ItemStack stack) {
        super.applyItemTransform(poseStack, isRightHand, stack);

        UseAnim anim = stack.getUseAnimation();
        // No rotar extra si es un arco o se está bloqueando
        if (anim == UseAnim.BOW || anim == UseAnim.BLOCK) return;

        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        poseStack.translate(0, 0.08, -0.05);
    }

    @Override
    protected void applyEmptyHandTransform(PoseStack poseStack, boolean isRightHand) {
        poseStack.mulPose(Axis.XP.rotationDegrees(isRightHand ? 60.0f : 150.0f));
        if (isRightHand) {
            poseStack.translate(0.12, 0, 0);
        }
    }

    // ── Renderizado en Tercera Persona ────────────────────────────────────────

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isRightHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isRightHand, isOffHand);

        // Ajuste para la mano izquierda con escudo/ítem
        if (!isRightHand && isOffHand) {
            poseStack.mulPose(Axis.YP.rotationDegrees(120.0f));
            return;
        }

        // Ajuste para mano izquierda relajada
        if (!isRightHand) {
            poseStack.translate(0, 0.3, -0.15);
            poseStack.mulPose(Axis.XP.rotationDegrees(-45.0f));
            return;
        }

        // Ajuste para mano derecha relajada
        if (!isOffHand) {
            poseStack.translate(-0.025, -0.05, 0);
        }
    }
}