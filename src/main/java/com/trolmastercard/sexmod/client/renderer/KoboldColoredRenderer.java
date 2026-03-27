package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.CoreGeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Ported from dj.java (1.12.2 - 1.20.1)
 * Full kobold entity renderer with:
 *  - Two static color-bone sets (mainColor bones in {@link #MAIN_COLOR_BONES},
 *    secondaryColor bones in {@link #SECONDARY_COLOR_BONES}).
 *  - Per-bone color lookup via {@link NpcColoredRenderer#getBoneColor(String)}.
 *  - Name-tag rendering that appends the tribe name in color.
 *  - Mouth-open UV shift when animation data index 7 == 1.
 *  - blowOpening bone UV zeroed out.
 *  - Custom model-change detection to clear the bone color cache.
 *
 * Original: {@code class dj extends d6<ff>}  (d6 = NpcColoredRenderer, ff = KoboldEntity)
 */
public class KoboldColoredRenderer extends NpcColoredRenderer<KoboldEntity> {

    // -- Bone color sets -------------------------------------------------------

    /** Bones that receive the kobold's MAIN body colour. */
    public static final HashSet<String> MAIN_COLOR_BONES = new HashSet<>(Arrays.asList(
            "colorSpots", "neck",       "head",        "snout",
            "midSectionR", "midSectionL", "innerCheekLR", "innerCheekRR",
            "gayL",        "gayR",
            "legR",        "legL",        "shinL",       "toesL",       "kneeL",  "curvesL",
            "shinR",       "toesR",       "kneeR",       "curvesR",
            "sideL",       "sideR",       "hip",
            "torsoL",      "torsoR",
            "armR",        "lowerArmR",   "ellbowR",
            "armL",        "lowerArmL",   "ellbowL",
            "hornUL",      "hornUR",
            "tail",        "tail2",       "tail3",       "tail4",       "tail5",
            "hornDL2",     "hornDR2",     "hornDR3M",    "hornDL3M",
            "frecklesAL1", "frecklesAL2", "frecklesAR1", "frecklesAR2",
            "frecklesHL1", "frecklesHL2", "frecklesHR1", "frecklesHR2"
    ));

    /** Bones that receive the kobold's SECONDARY body colour. */
    public static final HashSet<String> SECONDARY_COLOR_BONES = new HashSet<>(Arrays.asList(
            "boobR",          "boobL",          "frontNeck",
            "Rside",          "Lside",           "frontAndInside",
            "innerCheekLL",   "innerCheekRL",
            "layer",          "layer2",
            "down",           "down2",           "down3",           "down4",   "down5",
            "fuckhole",
            "hornDR3S",       "hornDL3S",
            "assholeCoverUp", "assholeCoverUp2"
    ));

    // -- State -----------------------------------------------------------------

    /** Cached world-space position used for physics/shaking effects. */
    private org.joml.Vector3f lastWorldPos;

    /** Tracks the model name to detect model-data changes and bust the colour cache. */
    private String lastModelName = null;

    // -- Constructor -----------------------------------------------------------

    public KoboldColoredRenderer(EntityRendererProvider.Context context,
                                  GeoModel<KoboldEntity> model,
                                  double shadowRadius) {
        super(context, model, shadowRadius);
    }

    // -- Colour lookup (overrides NpcColoredRenderer) --------------------------

    /**
     * Returns the Vec3i (r,g,b 0-255) colour for a bone by name.
     * Priority: mainColor - secondaryColor - irisL/irisR (eye colour) - white.
     */
    @Override
    protected net.minecraft.core.Vec3i getBoneColorByName(String boneName) {
        var data  = entity.getEntityData();
        EyeAndKoboldColor kc  = EyeAndKoboldColor.valueOf(data.get(KoboldEntity.BODY_COLOR));
        net.minecraft.core.BlockPos eyeColor = data.get(KoboldEntity.EYE_COLOR);

        if (MAIN_COLOR_BONES.contains(boneName))      return kc.getMainColor();
        if (SECONDARY_COLOR_BONES.contains(boneName)) return kc.getSecondaryColor();
        if ("irisL".equals(boneName) || "irisR".equals(boneName)) {
            // eye color stored as BlockPos (r,g,b in xyz)
            return eyeColor;
        }
        return WHITE; // default
    }

    // -- Bone hooks ------------------------------------------------------------

    /**
     * Intercepts specific bones for UV-offset adjustments.
     * - "blowOpening" - UV shift zeroed.
     * - "mouth"       - UV shifted -0.078125 when mouth-open flag is set.
     */
    @Override
    protected void onBonePreRender(CoreGeoBone bone,
                                   float red, float green, float blue,
                                   double uvOffset) {
        String name = bone.getName();
        if ("blowOpening".equals(name)) {
            // force UV offset to 0 for this bone (original set paramDouble = 0)
            super.onBonePreRender(bone, red, green, blue, 0.0);
            return;
        }
        if ("mouth".equals(name)) {
            String[] animData = NpcDataKeys.getCustomizationData(entity);
            int mouthFlag = Integer.parseInt(animData[7]);
            if (mouthFlag == 1) {
                super.onBonePreRender(bone, red, green, blue, -0.078125);
                return;
            }
        }
        super.onBonePreRender(bone, red, green, blue, uvOffset);
    }

    // -- Pre/post render scale -------------------------------------------------

    /**
     * Scale DOWN before render (apply hatch-progress shrink).
     * Original used {@code e7.aA} (spawnProgress DataParameter).
     */
    @Override
    protected void applyPreRenderScale(PoseStack poseStack) {
        float shrink = 0.25F - entity.getEntityData().get(KoboldEntity.SPAWN_PROGRESS);
        float s = 1.0F - shrink;
        poseStack.scale(s, s, s);
    }

    /** Scale back UP after render so subsequent drawing is at normal size. */
    @Override
    protected void applyPostRenderScale(PoseStack poseStack) {
        float shrink = 0.25F - entity.getEntityData().get(KoboldEntity.SPAWN_PROGRESS);
        double inv = 1.0 / (1.0 - shrink);
        poseStack.scale((float) inv, (float) inv, (float) inv);
    }

    // -- Held item override ----------------------------------------------------

    /**
     * Override which item appears in the kobold's hand based on AnimState:
     *  - STARTBLOWJOB - bow (has_arrows ? arrow : nothing)
     *  - ANAL_START   - arrow (count 3)
     *  - BOW          - arrow
     */
    @Override
    protected ItemStack resolveHeldItem(ItemStack original) {
        switch (entity.getAnimState()) {
            case STARTBLOWJOB -> {
                boolean hasArrows = entity.getEntityData().get(KoboldEntity.HAS_ARROWS);
                return new ItemStack(hasArrows ? Items.ARROW : Items.BOW);
            }
            case ANAL_START -> { return new ItemStack(Items.ARROW, 3); }
            case BOW        -> { return new ItemStack(Items.ARROW); }
            default         -> { return original; }
        }
    }

    // -- Render entry ---------------------------------------------------------

    @Override
    public void render(KoboldEntity kobold,
                       float entityYaw,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight) {
        entity = kobold;

        // Detect model-data change - bust colour cache
        String currentModel = kobold.getEntityData().get(BaseNpcEntity.MODEL_DATA);
        if (lastModelName != null && !lastModelName.equals(currentModel)) {
            ColoredNpcHandRenderer.clearColorCache();
        }
        lastModelName = currentModel;

        // Cache world position for physics
        lastWorldPos = new org.joml.Vector3f(
                (float) kobold.getX(), (float) kobold.getY(), (float) kobold.getZ());

        super.render(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // -- Name-tag -------------------------------------------------------------

    /**
     * Appends the kobold's tribe name (in its text colour) to the vanilla name tag.
     * Skips rendering when the tribe name is "null".
     */
    @Override
    protected void renderNameTag(KoboldEntity kobold,
                                 net.minecraft.network.chat.Component displayName,
                                 PoseStack poseStack,
                                 MultiBufferSource bufferSource,
                                 int packedLight) {
        if (kobold.isInvisible()) return;
        if (kobold.getAnimState().hideNameTag) return;

        var data = kobold.getEntityData();
        String tribeName = data.get(KoboldEntity.TRIBE_NAME_TAG);
        if ("null".equals(tribeName)) {
            super.renderNameTag(kobold, displayName, poseStack, bufferSource, packedLight);
            return;
        }

        EyeAndKoboldColor kc = EyeAndKoboldColor.valueOf(data.get(KoboldEntity.BODY_COLOR));
        String tag = kc.getTextColor() + " -" + tribeName + "-";
        var combined = net.minecraft.network.chat.Component.literal(kobold.getName().getString() + tag);

        super.renderNameTag(kobold, combined, poseStack, bufferSource, packedLight);
    }
}
