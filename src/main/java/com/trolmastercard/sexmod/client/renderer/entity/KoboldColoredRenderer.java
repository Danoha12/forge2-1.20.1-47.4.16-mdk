package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.renderer.ColoredNpcHandRenderer;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import com.trolmastercard.sexmod.util.NpcDataKeys;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * KoboldColoredRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja el coloreado por hueso, escalas de crecimiento y etiquetas de tribu.
 */
public class KoboldColoredRenderer extends NpcColoredRenderer<KoboldEntity> {

    // ── Clasificación de Huesos por Color ────────────────────────────────────

    public static final Set<String> MAIN_COLOR_BONES = new HashSet<>(Arrays.asList(
            "colorSpots", "neck", "head", "snout", "midSectionR", "midSectionL", "innerCheekLR", "innerCheekRR",
            "gayL", "gayR", "legR", "legL", "shinL", "toesL", "kneeL", "curvesL",
            "shinR", "toesR", "kneeR", "curvesR", "sideL", "sideR", "hip", "torsoL", "torsoR",
            "armR", "lowerArmR", "ellbowR", "armL", "lowerArmL", "ellbowL",
            "hornUL", "hornUR", "tail", "tail2", "tail3", "tail4", "tail5",
            "hornDL2", "hornDR2", "hornDR3M", "hornDL3M",
            "frecklesAL1", "frecklesAL2", "frecklesAR1", "frecklesAR2",
            "frecklesHL1", "frecklesHL2", "frecklesHR1", "frecklesHR2"
    ));

    public static final Set<String> SECONDARY_COLOR_BONES = new HashSet<>(Arrays.asList(
            "boobR", "boobL", "frontNeck", "Rside", "Lside", "frontAndInside",
            "innerCheekLL", "innerCheekRL", "layer", "layer2", "down", "down2",
            "down3", "down4", "down5", "fuckhole", "hornDR3S", "hornDL3S",
            "assholeCoverUp", "assholeCoverUp2"
    ));

    private String lastModelName = null;
    private Vector3f lastWorldPos;

    public KoboldColoredRenderer(EntityRendererProvider.Context context, GeoModel<KoboldEntity> model, double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // ── Lógica de Colores ────────────────────────────────────────────────────

    @Override
    protected net.minecraft.core.Vec3i getBoneColorByName(String boneName) {
        if (entity == null) return WHITE;

        EyeAndKoboldColor bodyColor = EyeAndKoboldColor.safeValueOf(entity.getEntityData().get(KoboldEntity.BODY_COLOR));
        net.minecraft.core.BlockPos eyeColor = entity.getEntityData().get(KoboldEntity.EYE_COLOR);

        if (MAIN_COLOR_BONES.contains(boneName)) return bodyColor.getMainColor();
        if (SECONDARY_COLOR_BONES.contains(boneName)) return bodyColor.getSecondaryColor();

        // El iris usa el color de ojos guardado en el BlockPos (X=R, Y=G, Z=B)
        if (boneName.equals("irisL") || boneName.equals("irisR")) return eyeColor;

        return WHITE;
    }

    // ── Hooks de Huesos y UV ─────────────────────────────────────────────────

    @Override
    protected void onBonePreRender(CoreGeoBone bone, float red, float green, float blue, double uvOffset) {
        String name = bone.getName();

        if (name.equals("blowOpening")) {
            super.onBonePreRender(bone, red, green, blue, 0.0);
            return;
        }

        if (name.equals("mouth")) {
            String[] customData = NpcDataKeys.getCustomizationData(entity);
            if (customData != null && customData.length > 7) {
                int mouthOpen = Integer.parseInt(customData[7]);
                if (mouthOpen == 1) {
                    // Desplazamiento vertical para mostrar la boca abierta en la textura
                    super.onBonePreRender(bone, red, green, blue, -0.078125);
                    return;
                }
            }
        }
        super.onBonePreRender(bone, red, green, blue, uvOffset);
    }

    // ── Escalado de Crecimiento (Spawn Progress) ─────────────────────────────

    @Override
    protected void applyPreRenderScale(PoseStack poseStack) {
        // Los Kobolds nacen pequeños y crecen hasta su 100% (0.25 offset original)
        float spawnProgress = entity.getEntityData().get(KoboldEntity.SPAWN_PROGRESS);
        float shrinkFactor = 1.0F - (0.25F - spawnProgress);
        poseStack.scale(shrinkFactor, shrinkFactor, shrinkFactor);
    }

    @Override
    protected void applyPostRenderScale(PoseStack poseStack) {
        float spawnProgress = entity.getEntityData().get(KoboldEntity.SPAWN_PROGRESS);
        float inverseScale = 1.0F / (1.0F - (0.25F - spawnProgress));
        poseStack.scale(inverseScale, inverseScale, inverseScale);
    }

    // ── Resolución de Ítems Visuales ─────────────────────────────────────────

    @Override
    protected ItemStack resolveHeldItem(ItemStack original) {
        if (entity == null) return original;

        AnimState state = entity.getAnimState();
        return switch (state) {
            case INTERACTION_A_START -> {
                boolean hasSpecialItem = entity.getEntityData().get(KoboldEntity.HAS_ARROWS);
                yield new ItemStack(hasSpecialItem ? Items.ARROW : Items.BOW);
            }
            case INTERACTION_B_START -> new ItemStack(Items.ARROW, 3);
            case BOW -> new ItemStack(Items.ARROW);
            default -> original;
        };
    }

    // ── Renderizado de Etiquetas ─────────────────────────────────────────────

    @Override
    protected void renderNameTag(KoboldEntity kobold, Component displayName, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (kobold.isInvisible() || kobold.getAnimState().isHideNameTag()) return;

        String tribeTag = kobold.getEntityData().get(KoboldEntity.TRIBE_NAME_TAG);
        if (tribeTag == null || tribeTag.equals("null")) {
            super.renderNameTag(kobold, displayName, poseStack, bufferSource, packedLight);
            return;
        }

        EyeAndKoboldColor tribeColor = EyeAndKoboldColor.safeValueOf(kobold.getEntityData().get(KoboldEntity.BODY_COLOR));
        Component coloredTag = Component.literal(" -" + tribeTag + "-").withStyle(tribeColor.getTextStyle());
        Component combinedName = Component.literal(kobold.getName().getString()).append(coloredTag);

        super.renderNameTag(kobold, combinedName, poseStack, bufferSource, packedLight);
    }

    @Override
    public void render(KoboldEntity kobold, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Limpiar caché de color si el modelo cambia (para evitar fantasmas visuales)
        String currentModel = kobold.getEntityData().get(BaseNpcEntity.MODEL_DATA);
        if (lastModelName != null && !lastModelName.equals(currentModel)) {
            ColoredNpcHandRenderer.clearColorCache();
        }
        lastModelName = currentModel;

        lastWorldPos = new Vector3f((float) kobold.getX(), (float) kobold.getY(), (float) kobold.getZ());
        super.render(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}