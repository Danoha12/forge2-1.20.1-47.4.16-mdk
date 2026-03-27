package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.trolmastercard.sexmod.item.GalathCoinItem;
import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoModel;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * GalathCoinRenderer - ported from av.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * The Galath Coin item has two rendering layers:
 *  1. A normal textured body (all bones except "pentagram")
 *  2. A glowing pentagram bone rendered with a pulsing emissive color and
 *     a configurable block-light level, driven by NBT timestamps stored on
 *     the player that holds the coin.
 *
 * NBT keys on the holding player's persistent data:
 *  - {@code sexmod:galath_coin_activation_time}   - System.currentTimeMillis() when activated
 *  - {@code sexmod:galath_coin_deactivation_time} - System.currentTimeMillis() when deactivated
 */
@OnlyIn(Dist.CLIENT)
public class GalathCoinRenderer extends GeoItemRenderer<GalathCoinItem> {

    // =========================================================================
    //  Color constants  (f7 - RgbColor)
    // =========================================================================

    /** Active pentagram color - hot pink. */
    public static final RgbColor COLOR_ACTIVE   = new RgbColor(0.84705883F, 0.11764706F, 0.35686275F);

    /** Inactive pentagram color - grey. */
    public static final RgbColor COLOR_INACTIVE = new RgbColor(0.44705883F, 0.44705883F, 0.44705883F);

    // =========================================================================
    //  Light level constants
    // =========================================================================

    /** Full-bright sky-light level (both sky + block channels packed). */
    public static final float LIGHT_FULL   = 240.0F;

    /** Half-bright level used during inactive state. */
    public static final float LIGHT_HALF   = 120.0F;

    /** Blend window in ms for activation/deactivation transitions. */
    private static final float TRANSITION_START_MS = 1000.0F;
    private static final float TRANSITION_END_MS   = 3000.0F;

    // =========================================================================
    //  Per-frame render state
    // =========================================================================

    /** True when currently rendering the pentagram bone (emissive pass). */
    private boolean renderingPentagram = false;

    /** Current pentagram emissive color (resolved each frame). */
    private RgbColor pentagramColor;

    private static final Minecraft MC = Minecraft.getInstance();

    // =========================================================================
    //  Constructor
    // =========================================================================

    public GalathCoinRenderer() {
        super(new GalathCoinModel());
    }

    // =========================================================================
    //  Main render override
    // =========================================================================

    /**
     * Two-pass render:
     *  Pass 1 - all normal bones (textured, lit by block light)
     *  Pass 2 - "pentagram" bone only (emissive, custom light + color)
     */
    @Override
    public void renderModel(GeoModel model,
                            GalathCoinItem item,
                            float limbSwing, float limbSwingAmount,
                            float ageInTicks,
                            float netHeadYaw, float headPitch,
                            PoseStack poseStack,
                            VertexConsumer buffer,
                            int packedLight, int packedOverlay,
                            float red, float green, float blue, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GeoBone pentagram = null;
        renderingPentagram = false;

        // -- Pass 1: normal body bones -------------------------------------
        GeoBone root = model.getTopLevelBones().get(0);
        MATRIX_STACK.pushPose();
        translateToBone(MATRIX_STACK, root);
        moveToPivot(MATRIX_STACK, root);
        rotateBone(MATRIX_STACK, root);
        scaleBone(MATRIX_STACK, root);
        moveBackFromPivot(MATRIX_STACK, root);

        for (GeoBone child : root.getChildBones()) {
            if ("pentagram".equals(child.getName())) {
                pentagram = child;
                continue;
            }
            renderBoneRecursive(child, buffer, packedLight, packedOverlay,
                red, green, blue, alpha);
        }

        // -- Pass 2: pentagram bone (emissive) -----------------------------
        float emissiveLight = resolveEmissiveLight(limbSwing);

        pentagramColor = resolvePentagramColor();

        // Override block-light channel to emissive level
        int emissivePackedLight = packLight(emissiveLight, emissiveLight);

        if (!GalathCoinItem.isGloballyDisabled()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableLighting();
        }

        if (pentagram != null) {
            renderingPentagram = true;
            renderBoneRecursive(pentagram, buffer, emissivePackedLight, packedOverlay,
                red, green, blue, alpha);
        }

        RenderSystem.enableLighting();
        MATRIX_STACK.popPose();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // =========================================================================
    //  Cube render (override - emissive path uses flat color, normal path uses
    //  standard POSITION_COLOR_TEX_NORMAL format)
    // =========================================================================

    @Override
    public void renderCube(GeoCube cube,
                           PoseStack poseStack,
                           VertexConsumer buffer,
                           int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {
        MATRIX_STACK.moveToPivot(cube);
        MATRIX_STACK.rotate(cube);
        MATRIX_STACK.moveBackFromPivot(cube);

        if (renderingPentagram) {
            renderCubeEmissive(cube, buffer);
            return;
        }

        // Normal textured path - mirrors original bytecode
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;

            Vector3f normal = new Vector3f(quad.normal().x(), quad.normal().y(), quad.normal().z());
            MATRIX_STACK.getNormalMatrix().transform(normal);

            // Same degenerate-normal fixup as in KoboldRenderer
            if (cube.size().y() == 0.0F && cube.size().z() == 0.0F && normal.x < 0.0F)
                normal.x = -normal.x;
            if (cube.size().x() == 0.0F && cube.size().z() == 0.0F && normal.y < 0.0F)
                normal.y = -normal.y;
            if (cube.size().x() == 0.0F && cube.size().y() == 0.0F && normal.z < 0.0F)
                normal.z = -normal.z;

            for (GeoVertex vertex : quad.vertices()) {
                Vector4f pos = new Vector4f(vertex.position().x(), vertex.position().y(),
                    vertex.position().z(), 1.0F);
                MATRIX_STACK.getModelMatrix().transform(pos);

                buffer.vertex(pos.x(), pos.y(), pos.z())
                      .uv(vertex.texU(), vertex.texV())
                      .overlayCoords(packedOverlay)
                      .uv2(packedLight)
                      .color(red, green, blue, alpha)
                      .normal(normal.x, normal.y, normal.z)
                      .endVertex();
            }
        }
    }

    /** Renders a cube using flat emissive color (no texture, no lighting). */
    private void renderCubeEmissive(GeoCube cube, VertexConsumer buffer) {
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            for (GeoVertex vertex : quad.vertices()) {
                Vector4f pos = new Vector4f(vertex.position().x(), vertex.position().y(),
                    vertex.position().z(), 1.0F);
                MATRIX_STACK.getModelMatrix().transform(pos);

                // Original: bufferBuilder.pos(x,y,z).tex(u,v).color(r,g,b,1).endVertex()
                buffer.vertex(pos.x(), pos.y(), pos.z())
                      .uv(vertex.texU(), vertex.texV())
                      .color(pentagramColor.r(), pentagramColor.g(), pentagramColor.b(), 1.0F)
                      .endVertex();
            }
        }
    }

    // =========================================================================
    //  Light level resolution (replaces float a(float) + overloads)
    // =========================================================================

    /**
     * Resolves the packed block-light level to use for the pentagram emissive pass.
     *
     * Logic:
     *  - If the coin is not held by the local player - idle sine wave
     *  - Activation transition (1s-3s after activation_time): 240-120
     *  - Deactivation transition (1s-3s after deactivation_time): 120-240
     *  - If globally active - LIGHT_HALF
     *  - Otherwise - idle sine wave
     */
    private float resolveEmissiveLight(float partialTick) {
        if (!isHeldByLocalPlayer()) return idleLightSine(partialTick);

        long now         = System.currentTimeMillis();
        CompoundTag data = MC.player.getPersistentData();
        long activateAt   = data.getLong("sexmod:galath_coin_activation_time");
        long deactivateAt = data.getLong("sexmod:galath_coin_deactivation_time");

        if (activateAt != 0L)   return lightActivate(now, activateAt, partialTick);
        if (deactivateAt != 0L) return lightDeactivate(now, deactivateAt, partialTick);
        if (GalathCoinItem.isGloballyActive()) return LIGHT_HALF;
        return idleLightSine(partialTick);
    }

    /** Transition from full-bright - half during the 1s-3s window after activation. */
    private float lightActivate(long now, long activatedAt, float partialTick) {
        float elapsed = (float)(now - activatedAt);
        if (elapsed < TRANSITION_START_MS) return LIGHT_FULL;
        if (elapsed <= TRANSITION_END_MS)
            return MathUtil.lerp(LIGHT_FULL, LIGHT_HALF,
                (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
        return LIGHT_HALF;
    }

    /** Transition from half-bright - full during the 1s-3s window after deactivation. */
    private float lightDeactivate(long now, long deactivatedAt, float partialTick) {
        float elapsed = (float)(now - deactivatedAt);
        if (elapsed < TRANSITION_START_MS) return LIGHT_HALF;
        if (elapsed <= TRANSITION_END_MS)
            return MathUtil.lerp(LIGHT_HALF, LIGHT_FULL,
                (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
        return LIGHT_FULL;
    }

    /** Idle sine-wave light pulse (60 units amplitude, period ~126 ticks). */
    private float idleLightSine(float partialTick) {
        return (float)(60.0 * Math.sin(
            (MC.player.tickCount + partialTick) * 0.05F) + 180.0);
    }

    // =========================================================================
    //  Color resolution (replaces f7 a() + overloads)
    // =========================================================================

    /**
     * Resolves the pentagram's emissive color, interpolating between
     * {@link #COLOR_ACTIVE} and {@link #COLOR_INACTIVE} during transitions.
     */
    private RgbColor resolvePentagramColor() {
        if (!isHeldByLocalPlayer()) return COLOR_ACTIVE;

        long now         = System.currentTimeMillis();
        CompoundTag data = MC.player.getPersistentData();
        long activateAt   = data.getLong("sexmod:galath_coin_activation_time");
        long deactivateAt = data.getLong("sexmod:galath_coin_deactivation_time");

        if (activateAt != 0L)   return colorActivate(activateAt, now);
        if (deactivateAt != 0L) return colorDeactivate(deactivateAt, now);
        if (GalathCoinItem.isGloballyActive()) return COLOR_INACTIVE;
        return COLOR_ACTIVE;
    }

    /** Interpolates inactive-active during the 1s-3s window after activation. */
    private RgbColor colorActivate(long activatedAt, long now) {
        float elapsed = (float)(now - activatedAt);
        if (elapsed < TRANSITION_START_MS) return COLOR_INACTIVE;
        if (elapsed <= TRANSITION_END_MS)
            return RgbColor.lerp(COLOR_INACTIVE, COLOR_ACTIVE,
                (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
        return COLOR_ACTIVE;
    }

    /** Interpolates active-inactive during the 1s-3s window after deactivation. */
    private RgbColor colorDeactivate(long deactivatedAt, long now) {
        float elapsed = (float)(now - deactivatedAt);
        if (elapsed < TRANSITION_START_MS) return COLOR_ACTIVE;
        if (elapsed <= TRANSITION_END_MS)
            return RgbColor.lerp(COLOR_ACTIVE, COLOR_INACTIVE,
                (elapsed - TRANSITION_START_MS) / (TRANSITION_END_MS - TRANSITION_START_MS));
        return COLOR_INACTIVE;
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private boolean isHeldByLocalPlayer() {
        if (MC.player == null || currentItemStack == null) return false;
        return MC.player.getMainHandItem() == currentItemStack
            || MC.player.getOffhandItem() == currentItemStack;
    }

    private static int packLight(float sky, float block) {
        return ((int)(sky) & 0xFFFF) | (((int)(block) & 0xFFFF) << 16);
    }

    // Bone-stack helpers (same pattern as KoboldRenderer)
    private void translateToBone(PoseStack ps, GeoBone b) {
        ps.translate(b.getPivotX()/16f, b.getPivotY()/16f, b.getPivotZ()/16f); }
    private void moveToPivot(PoseStack ps, GeoBone b) {
        ps.translate(-b.getPivotX()/16f, -b.getPivotY()/16f, -b.getPivotZ()/16f); }
    private void rotateBone(PoseStack ps, GeoBone b) {
        if (b.getRotZ()!=0) ps.mulPose(com.mojang.math.Axis.ZP.rotation(b.getRotZ()));
        if (b.getRotY()!=0) ps.mulPose(com.mojang.math.Axis.YP.rotation(b.getRotY()));
        if (b.getRotX()!=0) ps.mulPose(com.mojang.math.Axis.XP.rotation(b.getRotX())); }
    private void scaleBone(PoseStack ps, GeoBone b) {
        ps.scale(b.getScaleX(), b.getScaleY(), b.getScaleZ()); }
    private void moveBackFromPivot(PoseStack ps, GeoBone b) {
        ps.translate(b.getPivotX()/16f, b.getPivotY()/16f, b.getPivotZ()/16f); }

    private void renderBoneRecursive(GeoBone bone, VertexConsumer buffer,
                                     int light, int overlay,
                                     float r, float g, float b, float a) {
        MATRIX_STACK.pushPose();
        translateToBone(MATRIX_STACK, bone);
        moveToPivot(MATRIX_STACK, bone);
        rotateBone(MATRIX_STACK, bone);
        scaleBone(MATRIX_STACK, bone);
        moveBackFromPivot(MATRIX_STACK, bone);
        if (!bone.isHidden()) {
            for (GeoCube cube : bone.getChildCubes()) {
                MATRIX_STACK.pushPose();
                renderCube(cube, MATRIX_STACK, buffer, light, overlay, r, g, b, a);
                MATRIX_STACK.popPose();
            }
        }
        if (!bone.childBonesAreHiddenToo()) {
            for (GeoBone child : bone.getChildBones())
                renderBoneRecursive(child, buffer, light, overlay, r, g, b, a);
        }
        MATRIX_STACK.popPose();
    }
}
