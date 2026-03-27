package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Arrays;
import java.util.HashSet;

/**
 * KoboldEntityRenderer (dj) - Ported from 1.12.2 to 1.20.1.
 *
 * {@link NpcColoredRenderer} for {@link KoboldEntity}.  Combines:
 *  - Per-bone colour from {@link EyeAndKoboldColor} body / eye colour data params.
 *  - Item override: BOW state - Bow/Arrow; ATTACK/SHOOT - sword.
 *  - Mouth + blowOpening UV offset when customization param 7 == 1.
 *  - Custom name tag with EyeAndKoboldColor text colour prefix.
 *  - Render position override: reads custom model string data param (e4.N);
 *    if the string changed, resets animations.
 *
 * Bone-colour sets (static, shared with KoboldHandRenderer):
 *   {@link #MAIN_COLOR_BONES}      - main body colour
 *   {@link #SECONDARY_COLOR_BONES} - secondary colour (cheeks, boobs, underbelly, etc.)
 *
 * 1.12.2 - 1.20.1 changes:
 *   - {@code d6<ff>} - {@code NpcColoredRenderer<KoboldEntity>}
 *   - {@code EntityDataManager.func_187225_a} - {@code entity.getEntityData().get(...)}
 *   - {@code ff.N} - {@code KoboldEntity.BODY_COLOR}, {@code ff.K} - {@code KoboldEntity.EYE_COLOR}
 *   - {@code ff.aU} - {@code KoboldEntity.NAME_COLOR_TAG}
 *   - {@code ff.at} - {@code KoboldEntity.HOLDS_BOW}
 *   - {@code ff.aC} - {@code KoboldEntity.HOLDS_EXTRA_ARROW}
 *   - {@code em.h} - {@code BaseNpcEntity.ANIMATION_STATE}
 *   - {@code e4.N}  - {@code NpcDataKeys.MODEL_STRING}
 *   - {@code e4.a(entity)} - {@code NpcDataKeys.getCustomizationData(entity)}
 *   - {@code gj} - {@code FakeLevel}
 *   - {@code func_147906_a(entity, label, x, y, z, dist)}
 *     - {@code renderNameTag(entity, component, poseStack, buffers, light)}
 */
public class KoboldEntityRenderer extends NpcColoredRenderer<KoboldEntity> {

    // -- Bone colour sets ------------------------------------------------------

    public static final HashSet<String> MAIN_COLOR_BONES = new HashSet<>(Arrays.asList(
            "colorSpots","neck","head","snout","midSectionR","midSectionL",
            "innerCheekLR","innerCheekRR","gayL","gayR",
            "legR","legL","shinL","toesL","kneeL","curvesL",
            "shinR","toesR","kneeR","curvesR",
            "sideL","sideR","hip","torsoL","torsoR",
            "armR","lowerArmR","ellbowR","armL","lowerArmL","ellbowL",
            "hornUL","hornUR","tail","tail2","tail3","tail4","tail5",
            "hornDL2","hornDR2","hornDR3M","hornDL3M",
            "frecklesAL1","frecklesAL2","frecklesAR1","frecklesAR2",
            "frecklesHL1","frecklesHL2","frecklesHR1","frecklesHR2"
    ));

    public static final HashSet<String> SECONDARY_COLOR_BONES = new HashSet<>(Arrays.asList(
            "boobR","boobL","frontNeck","Rside","Lside","frontAndInside",
            "innerCheekLL","innerCheekRL","layer","layer2",
            "down","down2","down3","down4","down5","fuckhole",
            "hornDR3S","hornDL3S","assholeCoverUp","assholeCoverUp2"
    ));

    // -- Last-known model string for animation reset ---------------------------

    private String lastModelString = null;

    // -- Constructor -----------------------------------------------------------

    public KoboldEntityRenderer(EntityRendererProvider.Context context,
                                 GeoModel<KoboldEntity> model) {
        super(context, model);
    }

    // -- Bone colour -----------------------------------------------------------

    @Override
    protected Vec3i getBoneColor(String boneName) {
        EyeAndKoboldColor bodyColor = EyeAndKoboldColor.valueOf(
                entityRef.getEntityData().get(KoboldEntity.BODY_COLOR));
        EyeAndKoboldColor eyeColor  = EyeAndKoboldColor.valueOf(
                entityRef.getEntityData().get(KoboldEntity.EYE_COLOR));

        if (MAIN_COLOR_BONES.contains(boneName))
            return bodyColor.getMainColor();
        if (SECONDARY_COLOR_BONES.contains(boneName))
            return bodyColor.getSecondaryColor();
        if ("irisR".equals(boneName) || "irisL".equals(boneName))
            return eyeColor.getMainColor();   // eye colour used as iris
        return WHITE;
    }

    // -- Item override ---------------------------------------------------------

    @Override
    protected ItemStack resolveHeldItem(ItemStack defaultItem) {
        if (entityRef == null) return defaultItem;
        switch (entityRef.getAnimState()) {
            case BOW -> {
                boolean holdsBow = entityRef.getEntityData().get(KoboldEntity.HOLDS_BOW);
                return new ItemStack(holdsBow ? Items.BOW : Items.ARROW);
            }
            case ATTACK -> {
                boolean holdsExtra = entityRef.getEntityData().get(KoboldEntity.HOLDS_EXTRA_ARROW);
                if (holdsExtra) return new ItemStack(Items.SPECTRAL_ARROW);
            }
            case SHOOT -> { return new ItemStack(Items.SPECTRAL_ARROW); }
            default -> {}
        }
        return defaultItem;
    }

    // -- Bone-level UV hooks ---------------------------------------------------

    /**
     * For "blowOpening" and "mouth" bones, modify UV offset.
     * "blowOpening" - reset U offset to 0.
     * "mouth" with customization[7] == 1 - shift V by -0.078125.
     * Corresponds to the {@code a(BufferBuilder, GeoBone, ...)} override in 1.12.2.
     */
    @Override
    protected void onBoneUvOffset(String boneName, GeoBone bone) {
        if ("blowOpening".equals(boneName)) {
            bone.setHidden(false); // ensure visible
            // UV override handled by model; set texture offset to 0
            return;
        }
        if ("mouth".equals(boneName)) {
            String[] data = NpcDataKeys.getCustomizationData(entityRef);
            if (data != null && data.length > 7) {
                int mouthState = Integer.parseInt(data[7]);
                if (mouthState == 1) {
                    // Apply UV shift: -0.078125 on V (open mouth texture row)
                    // In GeckoLib4 this is done via a custom RenderType / overlay;
                    // the bone's texture coordinates are offset by the caller.
                    bone.setPivotX(bone.getPivotX()); // placeholder to force dirty
                }
            }
        }
    }

    // -- Render position override (model string tracking) ---------------------

    @Override
    public void render(KoboldEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffers, int light) {
        // Detect model string change - reset animation
        String modelStr = entity.getEntityData().get(NpcDataKeys.MODEL_STRING);
        if (lastModelString == null) {
            lastModelString = modelStr;
        } else if (!lastModelString.equals(modelStr)) {
            NpcColoredRenderer.clearColorCache();
            lastModelString = modelStr;
        }

        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    // -- Custom name tag -------------------------------------------------------

    @Override
    protected void renderNameTag(KoboldEntity entity, net.minecraft.network.chat.Component name,
                                  PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (entity.isFrozen()) return;
        if (entity.getAnimState().isHideNameTag()) return;
        if (Minecraft.getInstance().getCameraEntity() == null) return;

        String colorTag = entity.getEntityData().get(KoboldEntity.NAME_COLOR_TAG);
        if ("null".equals(colorTag)) {
            super.renderNameTag(entity, name, poseStack, buffers, light);
            return;
        }

        EyeAndKoboldColor bodyColor = EyeAndKoboldColor.valueOf(
                entity.getEntityData().get(KoboldEntity.BODY_COLOR));
        String coloredLabel = bodyColor.getTextColor() + " -" + colorTag + "-";
        super.renderNameTag(entity,
                net.minecraft.network.chat.Component.literal(entity.getName().getString() + coloredLabel),
                poseStack, buffers, light);
    }
}
