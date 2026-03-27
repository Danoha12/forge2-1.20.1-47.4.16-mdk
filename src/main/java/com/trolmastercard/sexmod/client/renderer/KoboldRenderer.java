package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.trolmastercard.sexmod.client.entity.ClothingOverlayEntity;
import com.trolmastercard.sexmod.client.model.KoboldModel;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.network.packet.CustomizeNpcPacket;
import com.trolmastercard.sexmod.registry.ClothingSlot;
import com.trolmastercard.sexmod.registry.ModelWhitelist;
import com.trolmastercard.sexmod.util.LightUtil;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.math.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoModel;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * KoboldRenderer - ported from b.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * Responsibilities:
 *  - Renders the live KoboldEntity in the world, correctly positioning each
 *    clothing-overlay {@link ClothingOverlayEntity} on top of the host
 *  - Applies custom-bone matrix transforms for fitted clothing
 *  - Handles lighting overrides from the {@link LightingMode} system
 *  - Supports two "GUI preview" scale constants (SCALE_NORMAL / SCALE_LARGE)
 */
@OnlyIn(Dist.CLIENT)
public class KoboldRenderer extends GeoEntityRenderer<KoboldEntity> {

    // =========================================================================
    //  Scale sentinel values - match the obfuscated 1.876945F / 2.876945F
    // =========================================================================

    /** Scale used when rendering in the normal customisation GUI. */
    public static final float SCALE_NORMAL = 1.876945F;

    /** Scale used when rendering in the large "full body" GUI view. */
    public static final float SCALE_LARGE  = 2.876945F;

    // =========================================================================
    //  Bone-remap tables  (h / f from original)
    // =========================================================================

    /**
     * Bottom-clothing bones that should be hidden when custom leg clothes are worn.
     * Maps custom-clothing bone name - base-model bone name.
     */
    private final HashMap<String, String> lowerBodyRemap = new HashMap<>();

    /**
     * Top-clothing bones that should be hidden when custom top clothes are worn.
     */
    private final HashMap<String, String> upperBodyRemap = new HashMap<>();

    /**
     * Per-bone item-in-hand transform suppliers.
     * Key = bone name, Value = function(KoboldEntity) - PoseStack modifier.
     */
    private final HashMap<String, BoneTransformSupplier> boneTransformSuppliers = new HashMap<>();

    // =========================================================================
    //  Per-render state  (reset at the start of each doRender call)
    // =========================================================================

    /** The KoboldEntity currently being rendered (set during render, cleared after). */
    @Nullable
    private KoboldEntity currentKobold = null;

    /** Active custom-model bundle for the current entity (may be null). */
    @Nullable
    private ModelWhitelist.ModelBundle currentBundle = null;

    /**
     * Directional-light tint applied to each vertex.
     * Defaults to white (1,1,1); modified when a {@link LightingMode#SEXMOD}
     * override is active.
     */
    private Vector3f vertexTint = new Vector3f(1.0F, 1.0F, 1.0F);

    /**
     * When non-null, a SEXMOD-specific directional light vector is active and
     * overrides Minecraft's normal lighting for this entity.
     */
    @Nullable
    private Vector3f sexmodLightDir = null;

    /** True during one-frame rendering of overlay entities to suppress recursion guards. */
    public static boolean renderingOverlay = false;

    // =========================================================================
    //  Constructor
    // =========================================================================

    public KoboldRenderer(EntityRendererProvider.Context ctx, GeoModel<KoboldEntity> model) {
        super(ctx, model);
        initRemapTables();
    }

    /** Populates the bone-remap and per-bone transform tables. */
    private void initRemapTables() {
        // Lower body
        lowerBodyRemap.put("customLegL",  "legL");
        lowerBodyRemap.put("customShinL", "shinL");
        lowerBodyRemap.put("customLegR",  "legR");
        lowerBodyRemap.put("customShinR", "shinR");

        // Upper body
        upperBodyRemap.put("top",              "upperBody");
        upperBodyRemap.put("customArmL",       "armL");
        upperBodyRemap.put("customLowerArmL",  "lowerArmL");
        upperBodyRemap.put("customArmR",       "armR");
        upperBodyRemap.put("customLowerArmR",  "lowerArmR");

        // Per-bone item-in-hand transforms
        boneTransformSuppliers.put("lowerArmR",
            kobold -> HandItemUtil.getMainHandTransform(kobold));
        boneTransformSuppliers.put("lowerArmL",
            kobold -> HandItemUtil.getOffHandTransform(kobold));
    }

    // =========================================================================
    //  Frustum culling
    // =========================================================================

    @Override
    public boolean shouldRender(KoboldEntity entity, Frustum frustum,
                                double camX, double camY, double camZ) {
        return super.shouldRender(entity, frustum, camX, camY, camZ);
    }

    // =========================================================================
    //  Main render entry point
    // =========================================================================

    @Override
    public void render(KoboldEntity kobold,
                       float entityYaw,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight) {

        // Skip in model-selection mode
        if (ModelWhitelist.isSelectingModel()) return;

        // Apply GUI-scale sentinel guards
        if (!isValidRenderContext(partialTick)) return;

        // Handle clothing-texture swap when the clothing set has changed
        if (handleClothingSwap(kobold)) return;

        // Store current entity + resolve bundle
        currentKobold  = kobold;
        currentBundle  = ModelWhitelist.getBundleForTexture(kobold.getTextureName());

        // Resolve lighting override
        applyLightingOverride(currentBundle, kobold, partialTick);

        float[] savedAngles = saveRotationState(kobold);

        if (partialTick != SCALE_NORMAL && partialTick != SCALE_LARGE) {
            // Normal world render - snap to host entity's transform
            renderWorldInstance(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } else {
            // GUI preview render
            vertexTint = new Vector3f(1.0F, 1.0F, 1.0F);
            super.render(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }

        restoreRotationState(kobold, savedAngles);
        RenderSystem.enableDepthTest();
    }

    /** Renders the entity snapped to its host's world position (for overlay entities). */
    private void renderWorldInstance(KoboldEntity kobold,
                                     float entityYaw, float partialTick,
                                     PoseStack poseStack,
                                     MultiBufferSource bufferSource,
                                     int packedLight) {
        UUID ownerUUID = kobold.getOwnerUUID();
        if (ownerUUID == null) return;

        BaseNpcEntity owner = resolveOwner(kobold, ownerUUID);
        if (owner == null) return;

        // Verify custom-model visibility
        ModelWhitelist.ModelBundle bundle = currentBundle;
        if (bundle != null && !bundle.isVisible() && owner.getBodySizeScaled() == 0) return;

        // Resolve the living entity whose position/rotation we follow
        LivingEntity host = resolveHost(kobold, owner);

        // Interpolate host world position
        net.minecraft.world.phys.Vec3 hostPos = MathUtil.lerpPosition(
            new net.minecraft.world.phys.Vec3(owner.xOld, owner.yOld, owner.zOld),
            owner.position(), partialTick);

        // Compute camera-relative offset
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.phys.Vec3 camPos = MathUtil.lerpPosition(
            new net.minecraft.world.phys.Vec3(mc.player.xOld, mc.player.yOld, mc.player.zOld),
            mc.player.position(), partialTick);
        net.minecraft.world.phys.Vec3 offset = hostPos.subtract(camPos);

        // Update overlay entity transforms to match host
        snapToHost(kobold, host, partialTick);

        // Light from host position
        int light = LightUtil.getLightAt(host);
        float lightF = (float)(light & 0xFFFF) / 15.0F;
        vertexTint = new Vector3f(lightF, lightF, lightF);

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);
        if (owner.hasOrientationOverride()) {
            poseStack.mulPose(
                com.mojang.math.Axis.YP.rotationDegrees(owner.getOrientationYaw()));
        }
        super.render(kobold, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    // =========================================================================
    //  Static helper: render all overlays for a NPC (called from NPC renderer)
    // =========================================================================

    /**
     * Renders all active clothing-overlay entities that belong to {@code npc},
     * at the NPC's current world position.
     *
     * Equivalent to the original static {@code a(em, float)} method.
     */
    @OnlyIn(Dist.CLIENT)
    public static void renderOverlaysFor(BaseNpcEntity npc, float partialTick) {
        if (npc.isRemoved()) return;
        if (!npc.level().isClientSide()) return;
        if (!npc.hasActiveTextures()) return;

        var renderManager = Minecraft.getInstance().getEntityRenderDispatcher();
        for (String textureName : npc.getActiveTextures()) {
            ClothingOverlayEntity overlay = new ClothingOverlayEntity(
                npc.level(), npc.getUUID(), textureName);
            renderingOverlay = true;
            renderManager.render(overlay, 0.0D, 0.0D, 0.0D,
                0.0F, partialTick,
                new PoseStack(),
                Minecraft.getInstance().renderBuffers().bufferSource(),
                0xF000F0);
        }
    }

    // =========================================================================
    //  Bone-level render override (replaces original a(GeoModel,...) method)
    // =========================================================================

    @Override
    public void renderModel(GeoModel model,
                            KoboldEntity kobold,
                            float limbSwing, float limbSwingAmount,
                            float ageInTicks,
                            float netHeadYaw, float headPitch,
                            PoseStack poseStack,
                            VertexConsumer buffer,
                            int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (GeoBone bone : model.getTopLevelBones()) {
            // Apply custom-model matrix if applicable (GUI preview path)
            if (limbSwing != SCALE_NORMAL) {
                applyCustomBoneTransform(kobold, bone, limbSwing);
            }
            poseStack.translate(
                -bone.getPivotX() / 16.0F,
                -bone.getPivotY() / 16.0F,
                -bone.getPivotZ() / 16.0F);
            renderBoneRecursive(bone, poseStack, buffer, packedLight, packedOverlay,
                red, green, blue, alpha);
        }

        RenderSystem.disableBlend();
    }

    // =========================================================================
    //  Recursive bone render
    // =========================================================================

    /**
     * Renders a single bone and all of its children recursively, using the
     * entity's custom {@link ClothingOverlayEntity#matrixStack} for transform
     * accumulation.
     *
     * Equivalent to the original {@code renderRecursively(BufferBuilder, GeoBone, ...)} method.
     */
    public void renderBoneRecursive(GeoBone bone,
                                    PoseStack poseStack,
                                    VertexConsumer buffer,
                                    int packedLight, int packedOverlay,
                                    float r, float g, float b, float a) {
        if (currentKobold == null) return;
        PoseStack localStack = currentKobold.getBoneMatrixStack();
        if (localStack == null) return;

        localStack.pushPose();
        translateToBone(localStack, bone);
        moveToPivot(localStack, bone);
        rotateBone(localStack, bone);
        scaleBone(localStack, bone);
        moveBackFromPivot(localStack, bone);

        if (!bone.isHidden()) {
            for (GeoCube cube : bone.getChildCubes()) {
                poseStack.pushPose();
                renderCube(cube, localStack, buffer, packedLight, packedOverlay, r, g, b, a);
                poseStack.popPose();
            }
        }

        if (!bone.childBonesAreHiddenToo()) {
            for (GeoBone child : bone.getChildBones()) {
                renderBoneRecursive(child, poseStack, buffer,
                    packedLight, packedOverlay, r, g, b, a);
            }
        }

        localStack.popPose();
    }

    /**
     * Renders a single {@link GeoCube} by iterating its quads and submitting
     * vertices into the {@link VertexConsumer}.
     *
     * This reconstructs the bytecode-only {@code renderCube} from the original,
     * including the normal-sign fixup logic for degenerate quad normals.
     */
    private void renderCube(GeoCube cube,
                             PoseStack localStack,
                             VertexConsumer buffer,
                             int packedLight, int packedOverlay,
                             float red, float green, float blue, float alpha) {
        localStack.last().pose();   // ensure pivot/rotate/scale applied above
        moveToPivot(localStack, cube);
        rotateCube(localStack, cube);
        moveBackFromPivot(localStack, cube);

        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;

            // Transform the face normal
            Vector3f normal = new Vector3f(quad.normal().x(), quad.normal().y(), quad.normal().z());
            localStack.last().normal().transform(normal);

            // Normal-sign fixup for degenerate flat quads (matches original logic)
            if (cube.size().y() == 0.0F && cube.size().z() == 0.0F && normal.x() < 0.0F)
                normal.x = -normal.x;
            if (cube.size().x() == 0.0F && cube.size().z() == 0.0F && normal.y() < 0.0F)
                normal.y = -normal.y;
            if (cube.size().x() == 0.0F && cube.size().y() == 0.0F && normal.z() < 0.0F)
                normal.z = -normal.z;

            // Compute per-vertex tint from directional light
            Vector3f tint = sexmodLightDir != null
                ? computeSexmodTint(vertexTint, normal, sexmodLightDir)
                : vertexTint;

            for (GeoVertex vertex : quad.vertices()) {
                Vector4f pos = new Vector4f(
                    vertex.position().x(),
                    vertex.position().y(),
                    vertex.position().z(),
                    1.0F);
                localStack.last().pose().transform(pos);

                buffer.vertex(pos.x(), pos.y(), pos.z())
                      .uv(vertex.texU(), vertex.texV())
                      .overlayCoords(packedOverlay)
                      .uv2(packedLight)
                      .color(tint.x, tint.y, tint.z, alpha)
                      .normal(normal.x, normal.y, normal.z)
                      .endVertex();
            }
        }
    }

    // =========================================================================
    //  World-position resolver
    // =========================================================================

    /**
     * Computes the camera-relative render offset for a clothing overlay entity,
     * and snaps the overlay's position/rotation to match its host.
     *
     * Equivalent to the original static {@code a(Minecraft, cy, EntityLivingBase, em, float)}.
     */
    public static net.minecraft.world.phys.Vec3 computeOverlayOffset(
            Minecraft mc,
            ClothingOverlayEntity overlay,
            LivingEntity host,
            BaseNpcEntity owner,
            float partialTick) {

        net.minecraft.world.phys.Vec3 hostPos;

        if (owner.hasOrientationOverride()) {
            net.minecraft.world.phys.Vec3 snapPos = owner.getOverridePosition();
            float yaw = owner.getOrientationYaw();

            // Snap all position/rotation fields of the overlay to the fixed position
            overlay.xOld = overlay.yo = overlay.zOld = 0.0;
            overlay.setPos(snapPos.x, snapPos.y, snapPos.z);
            overlay.setYRot(yaw);
            overlay.yRotO = yaw;
            overlay.yBodyRot = yaw;
            overlay.yBodyRotO = yaw;
            overlay.yHeadRot = yaw;
            overlay.yHeadRotO = yaw;
            overlay.xRot = 0.0F;
            overlay.xRotO = 0.0F;

            hostPos = snapPos;
        } else {
            // Mirror all rotation/position fields from the host
            overlay.yRot    = host.yRot;
            overlay.yRotO   = host.yRotO;
            overlay.yBodyRot  = host.yBodyRot;
            overlay.yBodyRotO = host.yBodyRotO;
            overlay.yHeadRot  = host.yHeadRot;
            overlay.yHeadRotO = host.yHeadRotO;
            overlay.xRot    = host.xRot;
            overlay.xRotO   = host.xRotO;
            overlay.xOld    = host.xOld;
            overlay.yOld    = host.yOld;
            overlay.zOld    = host.zOld;
            overlay.setPos(host.getX(), host.getY(), host.getZ());

            hostPos = MathUtil.lerpPosition(
                new net.minecraft.world.phys.Vec3(host.xOld, host.yOld, host.zOld),
                host.position(), partialTick);
        }

        // Camera-relative offset
        net.minecraft.world.phys.Vec3 camPos = MathUtil.lerpPosition(
            new net.minecraft.world.phys.Vec3(
                mc.player.xOld, mc.player.yOld, mc.player.zOld),
            mc.player.position(), partialTick);

        return hostPos.subtract(camPos);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /** Returns true if this partialTick value represents a valid render context. */
    private boolean isValidRenderContext(float partialTick) {
        if (partialTick == SCALE_LARGE) return true;
        if (partialTick == SCALE_NORMAL) return true;
        if (renderingOverlay) { renderingOverlay = false; return true; }
        return false;
    }

    /**
     * Checks whether the overlay's texture is still valid for the host entity.
     * If not, sends a remove-texture packet and returns true (abort render).
     */
    private boolean handleClothingSwap(ClothingOverlayEntity overlay) {
        String textureName = overlay.getTextureName();
        try {
            if (overlay.isPreviewMode()) return false;
            if (ModelWhitelist.isLocalTexture(textureName)) return false;
            if (ModelWhitelist.getServerUrl() != null) return true;

            UUID ownerUUID = overlay.getOwnerUUID();
            BaseNpcEntity owner = BaseNpcEntity.getById(ownerUUID);
            if (owner == null) return true;

            // Check if this texture is still active on the owner
            HashSet<String> current = owner.getActiveTextures();
            current.remove(textureName);
            String updated = BaseNpcEntity.encodeTextures(current);
            ModNetwork.CHANNEL.sendToServer(new CustomizeNpcPacket(updated, overlay.getOwnerUUID()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Stub - handled by the KoboldEntity (was em.a(String, boolean) in original)
    private boolean handleClothingSwap(KoboldEntity kobold) { return false; }

    private void applyLightingOverride(@Nullable ModelWhitelist.ModelBundle bundle,
                                       KoboldEntity kobold, float partialTick) {
        if (bundle != null && bundle.getLightingMode() != LightingMode.DEFAULT) {
            RenderSystem.disableLighting();
            sexmodLightDir = (bundle.getLightingMode() == LightingMode.SEXMOD)
                ? LightUtil.computeSexmodLightDir(kobold, partialTick)
                : null;
        } else {
            sexmodLightDir = null;
        }
    }

    private void applyCustomBoneTransform(KoboldEntity kobold, GeoBone bone, float scale) {
        String boneName = resolveCustomBoneName(kobold);
        if (boneName == null) return;
        applyCustomBoneTransformForName(kobold, bone, scale, boneName);
    }

    private void applyCustomBoneTransformForName(KoboldEntity kobold, GeoBone bone,
                                                  float scale, String boneName) {
        BaseNpcEntity owner = resolveOwner(kobold);
        if (owner == null) return;
        PoseStack ms = owner.getMatrixStackForBone(boneName, false);
        kobold.setBoneMatrixStack(ms);

        if (kobold.isPreviewMode()) {
            if (scale == SCALE_LARGE) {
                ms.scale(0.5F, 0.5F, 0.5F);
                ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                    -com.trolmastercard.sexmod.client.gui.NpcCustomizeScreen.previewRotation));
            }
        }
    }

    @Nullable
    private String resolveCustomBoneName(KoboldEntity kobold) {
        if (kobold.isPreviewMode()) return kobold.getPreviewBoneName();
        ModelWhitelist.ModelBundle bundle =
            ModelWhitelist.getBundleForTexture(kobold.getTextureName());
        if (bundle == null) return null;
        if (ClothingSlot.CUSTOM_BONE.equals(bundle.getClothingSlot()))
            return bundle.getBoneName();
        return bundle.getClothingSlot().boneName;
    }

    @Nullable
    private BaseNpcEntity resolveOwner(KoboldEntity kobold) {
        UUID uuid = kobold.getOwnerUUID();
        return (uuid != null) ? BaseNpcEntity.getById(uuid) : null;
    }

    @Nullable
    private BaseNpcEntity resolveOwner(KoboldEntity kobold, UUID uuid) {
        BaseNpcEntity owner = PlayerKoboldEntity.getForUUID(uuid);
        return (owner != null) ? owner : BaseNpcEntity.getById(uuid);
    }

    @Nullable
    private LivingEntity resolveHost(KoboldEntity kobold, BaseNpcEntity owner) {
        if (!(owner instanceof PlayerKoboldEntity playerKobold)) return owner;
        UUID playerUUID = playerKobold.getPlayerUUID();
        if (playerUUID == null) return owner;
        Player player = kobold.level().getPlayerByUUID(playerUUID);
        return (player != null) ? player : owner;
    }

    private void snapToHost(KoboldEntity overlay, LivingEntity host, float partialTick) {
        overlay.yRot      = host.yRot;
        overlay.yRotO     = host.yRotO;
        overlay.yBodyRot  = host.yBodyRot;
        overlay.yBodyRotO = host.yBodyRotO;
        overlay.yHeadRot  = host.yHeadRot;
        overlay.yHeadRotO = host.yHeadRotO;
        overlay.xRot      = host.xRot;
        overlay.xRotO     = host.xRotO;
        overlay.xOld      = host.xOld;
        overlay.yOld      = host.yOld;
        overlay.zOld      = host.zOld;
        overlay.setPos(host.getX(), host.getY(), host.getZ());
    }

    private float[] saveRotationState(KoboldEntity e) {
        return new float[]{ e.yRot, e.yRotO, e.yHeadRot, e.yHeadRotO, e.yBodyRot, e.yBodyRotO,
                            e.xRot, e.xRotO };
    }

    private void restoreRotationState(KoboldEntity e, float[] s) {
        e.yRot = s[0]; e.yRotO = s[1]; e.yHeadRot = s[2]; e.yHeadRotO = s[3];
        e.yBodyRot = s[4]; e.yBodyRotO = s[5]; e.xRot = s[6]; e.xRotO = s[7];
    }

    private Vector3f computeSexmodTint(Vector3f base, Vector3f normal, Vector3f lightDir) {
        float dot = Math.max(0.0F, normal.dot(lightDir));
        return new Vector3f(base.x * dot, base.y * dot, base.z * dot);
    }

    // =========================================================================
    //  GeckoLib 4 bone-stack operations
    //  (replaces geckolib3 MatrixStack.translate / moveToPivot / rotate / etc.)
    // =========================================================================

    private void translateToBone(PoseStack ps, GeoBone bone) {
        ps.translate(bone.getPivotX() / 16.0F,
                     bone.getPivotY() / 16.0F,
                     bone.getPivotZ() / 16.0F);
    }

    private void moveToPivot(PoseStack ps, GeoBone bone) {
        ps.translate(-bone.getPivotX() / 16.0F,
                     -bone.getPivotY() / 16.0F,
                     -bone.getPivotZ() / 16.0F);
    }

    private void rotateBone(PoseStack ps, GeoBone bone) {
        if (bone.getRotZ() != 0) ps.mulPose(com.mojang.math.Axis.ZP.rotation(bone.getRotZ()));
        if (bone.getRotY() != 0) ps.mulPose(com.mojang.math.Axis.YP.rotation(bone.getRotY()));
        if (bone.getRotX() != 0) ps.mulPose(com.mojang.math.Axis.XP.rotation(bone.getRotX()));
    }

    private void scaleBone(PoseStack ps, GeoBone bone) {
        ps.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
    }

    private void moveBackFromPivot(PoseStack ps, GeoBone bone) {
        ps.translate(bone.getPivotX() / 16.0F,
                     bone.getPivotY() / 16.0F,
                     bone.getPivotZ() / 16.0F);
    }

    private void moveToPivot(PoseStack ps, GeoCube cube) {
        ps.translate(cube.pivot().x() / 16.0F,
                     cube.pivot().y() / 16.0F,
                     cube.pivot().z() / 16.0F);
    }

    private void rotateCube(PoseStack ps, GeoCube cube) {
        if (cube.rotation().z() != 0) ps.mulPose(com.mojang.math.Axis.ZP.rotation(cube.rotation().z()));
        if (cube.rotation().y() != 0) ps.mulPose(com.mojang.math.Axis.YP.rotation(cube.rotation().y()));
        if (cube.rotation().x() != 0) ps.mulPose(com.mojang.math.Axis.XP.rotation(cube.rotation().x()));
    }

    private void moveBackFromPivot(PoseStack ps, GeoCube cube) {
        ps.translate(-cube.pivot().x() / 16.0F,
                     -cube.pivot().y() / 16.0F,
                     -cube.pivot().z() / 16.0F);
    }

    // =========================================================================
    //  Functional interface for per-bone item transforms
    // =========================================================================

    @FunctionalInterface
    interface BoneTransformSupplier {
        void apply(KoboldEntity kobold);
    }

    // =========================================================================
    //  Lighting mode enum  (replaces c8)
    // =========================================================================

    public enum LightingMode {
        DEFAULT,
        SEXMOD
    }
}
