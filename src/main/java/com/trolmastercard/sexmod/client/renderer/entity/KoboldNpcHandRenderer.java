package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import com.trolmastercard.sexmod.util.NpcDataKeys;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.joml.Vector4f;
import software.bernie.geckolib.model.GeoModel;

/**
 * KoboldHandRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja el renderizado de extremidades con:
 * - Colores dinámicos por hueso.
 * - Desplazamiento de UV para la boca abierta/cerrada.
 * - Escalado dinámico basado en el progreso de spawn.
 */
public class KoboldHandRenderer extends ColoredNpcArmRenderer<KoboldEntity> {

    public KoboldHandRenderer(EntityRendererProvider.Context context, GeoModel<KoboldEntity> model) {
        super(context, model);
    }

    // ── Resolución de Colores Dinámicos ──────────────────────────────────────

    @Override
    protected Vec3i getBoneColor(String boneName) {
        if (entityRef == null) return WHITE;

        // Obtenemos los colores de los DataParameters de la entidad
        String bodyColorName = entityRef.getEntityData().get(KoboldEntity.BODY_COLOR);
        EyeAndKoboldColor bodyColor = EyeAndKoboldColor.safeValueOf(bodyColorName);

        // El color de ojos se guarda como Vec3i/BlockPos para el Iris
        Vec3i eyeColor = entityRef.getEntityData().get(KoboldEntity.EYE_COLOR);

        // Usamos los sets definidos en el renderer principal para consistencia
        if (KoboldEntityRenderer.MAIN_COLOR_BONES.contains(boneName)) {
            return bodyColor.getMainColor();
        }
        if (KoboldEntityRenderer.SECONDARY_COLOR_BONES.contains(boneName)) {
            return bodyColor.getSecondaryColor();
        }
        if (boneName.equals("irisL") || boneName.equals("irisR")) {
            return eyeColor;
        }

        return WHITE;
    }

    // ── Lógica de Expresión Facial (UV Shift) ────────────────────────────────

    /**
     * Aplica un desplazamiento en la textura para el hueso de la boca.
     * SFW: El flag 7 en animData controla si la boca está en pose de "interacción" o normal.
     */
    @Override
    protected Vector4f applyBoneUvModifier(String boneName, float r, float g, float b) {
        if (boneName.equals("mouth")) {
            String[] animData = NpcDataKeys.getCustomizationData(entityRef);
            if (animData != null && animData.length > 7) {
                int mouthFlag = Integer.parseInt(animData[7]);
                if (mouthFlag == 1) {
                    // El cuarto componente (w) se usa aquí para el offset en el eje V de la textura
                    return new Vector4f(r, g, b, -0.078125F);
                }
            }
        }
        return super.applyBoneUvModifier(boneName, r, g, b);
    }

    // ── Escalado de Crecimiento (Spawn Progress) ─────────────────────────────

    @Override
    protected void applyBaseTranslation(PoseStack poseStack) {
        // Obtenemos el progreso de crecimiento (0.0 a 0.25)
        float progress = entityRef.getEntityData().get(KoboldEntity.SPAWN_PROGRESS);
        float shrinkFactor = 1.0F - (0.25F - progress);

        poseStack.scale(shrinkFactor, shrinkFactor, shrinkFactor);
    }

    // ── Transformaciones de Ítems en Mano ─────────────────────────────────────

    @Override
    protected void applyHeldItemTransform(PoseStack poseStack, boolean isMainHand, ItemStack stack) {
        super.applyHeldItemTransform(poseStack, isMainHand, stack);

        if (!stack.isEmpty() && stack.getUseAnimation() == UseAnim.BOW) {
            // Lógica especial para cuando el Kobold apunta con el arco
            if (!isMainHand) {
                poseStack.mulPose(Axis.XP.rotationDegrees(170.0F));
            } else {
                poseStack.translate(0.1, 0.0, 0.0);
            }
            return;
        }

        // Rotación por defecto para armas estándar
        float angle = isMainHand ? 80.0F : 180.0F;
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));
    }

    @Override
    protected void applyThirdPersonTransform(PoseStack poseStack, boolean isMainHand, boolean isOffHand) {
        super.applyThirdPersonTransform(poseStack, isMainHand, isOffHand);

        if (isMainHand) {
            if (isOffHand) {
                // Pose de combate/dos manos
                poseStack.translate(0.06, 0.0, -0.13);
                poseStack.mulPose(Axis.YP.rotationDegrees(60.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(38.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            } else {
                // Pose relajada derecha
                poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
                poseStack.translate(0.0, -0.3, -0.13);
            }
        } else {
            if (isOffHand) {
                // Pose relajada izquierda con ítem
                poseStack.mulPose(Axis.YP.rotationDegrees(150.0F));
                poseStack.translate(0.0, -0.35, 0.0);
            } else {
                poseStack.translate(0.0, -0.1, -0.083);
            }
        }
    }
}