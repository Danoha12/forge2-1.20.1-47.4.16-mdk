package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;

/**
 * GalathRenderer (da) - GeoEntityRenderer for GalathEntity (f_).
 * Implements IBoneFilter (c3).
 *
 * Features:
 *  - Hair strand physics rendering via PhysicsParticleChain (ef) at
 *    "hairStrandStart/Mid/End R/L" bones
 *  - Wing mesh tessellation at "wingRV0-13" / "wingLV0-13" bone positions
 *  - Coin emission effect during GIVE_COIN AnimState
 *  - Tongue / mangTongue tentacle physics (ef.b chain)
 *  - Eye iris movement during MORNING_BLOWJOB states
 *  - Body position interpolation during FLY and approach states
 *  - Masturbate yaw sync (MASTERBATE state)
 *  - RAPE_CHARGE arm IK toward target
 *  - Star / pentagram ring particle rendering when player is bound
 *
 * Static hidden-bone set: static, turnable, slip, boobs, booty, vagina,
 * fuckhole, futaBallLR, futaBallLL, coin, pentagram
 * (merged with NpcBoneRegistry.hiddenBoneSet at first use).
 */
public class GalathRenderer extends BaseNpcRenderer<GalathEntity> implements IBoneFilter {

    // -- Constants -------------------------------------------------------------

    public static final int WING_VERTEX_COUNT  = 14;
    public static final HashSet<String> HIDDEN_BONES = new HiddenBoneSet();

    // RgbaColor constants for tongue/hair effects
    static final RgbaColor TONGUE_COLOR   = new RgbaColor(152, 45, 62, 255);
    static final RgbaColor TONGUE_SETTLED = new RgbaColor(84, 66, 88, 255);

    // UV anchors for wing geometry (YawPitch x and c fields are UV coords)
    static final YawPitch WING_UV_A = new YawPitch(0.25f, 0.125f);  // tip
    static final YawPitch WING_UV_B = new YawPitch(0.375f, 0.125f); // base
    static final float    WING_UV_STRIDE = 0.125f;

    // Tongue physics configs (base=G, settled=t)
    static final PhysicsParticleChain.Config TONGUE_CONFIG_G;
    static final PhysicsParticleChain.Config TONGUE_CONFIG_T;

    // Render state
    boolean colorCacheInitialised = false;
    float   lastYaw = 0f;

    public GalathRenderer(GeoModel<GalathEntity> model, double shadowRadius) {
        super(model, shadowRadius);
    }

    // -- Hidden bones (lazy-merged with NpcBoneRegistry) -----------------------

    @Override
    public HashSet<String> getHiddenBones() {
        if (!colorCacheInitialised) {
            HIDDEN_BONES.addAll(NpcBoneRegistry.EMPTY_SET);
            HIDDEN_BONES.addAll(PhysicsParticleRenderer.GALATH_HIDDEN);
            colorCacheInitialised = true;
        }
        return HIDDEN_BONES;
    }

    // -- Pre-render: body interpolation, yaw sync -----------------------------

    @Override
    public void preRender(PoseStack poseStack, GalathEntity entity, BakedGeoModel bakedModel,
                           MultiBufferSource bufferSource, RenderType renderType,
                           boolean isReRender, float partialTick,
                           int packedLight, int packedOverlay,
                           float red, float green, float blue, float alpha) {

        // FakeWorld skip
        if (entity.level() instanceof FakeWorld) return;
        // Null / hidden Galath skip
        if (entity.isSummonedAway()) return;

        // Compute interpolated body position during FLY / approach phases
        Vec3 bodyOffset = computeBodyOffset(entity, partialTick);
        if (bodyOffset != null) entity.setBodyOffset(bodyOffset);

        // Sync entity rotations
        entity.yBodyRot  = entity.yRotO;
        GalathEntity.syncPreRenderAngles(entity, partialTick);

        // Masturbate / RAPE_CHARGE yaw passes
        applyMasturbateYaw(entity);
        applyRapeChargeYaw(entity);

        // Wing overlay rendering (called before super so the geometry is in the right transform)
        if (entity.hasWings()) {
            PhysicsParticleRenderer.renderGalathWings(entity, bufferSource, poseStack, partialTick);
        }

        super.preRender(poseStack, entity, bakedModel, bufferSource, renderType,
            isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // Post-super: hair strand and star particle rendering
        renderHairAndEffects(entity, poseStack, bufferSource, partialTick);
    }

    // -- Per-bone overrides ----------------------------------------------------

    @Override
    protected void onBoneRender(PoseStack poseStack, GeoBone bone) {
        switch (bone.getName()) {
            // Hair physics: mirror head tilt onto hairBack and side strands
            case "hairBack" -> applyHairBackTilt(bone);
            case "hairDownSideL", "hairDownSideR" -> applyHairSideTilt(bone);
            // Head: MORNING_BLOWJOB sway
            case "head" -> {
                applyHeadTongueTrack(bone);
                applyFlyAndSwordHeadIK(bone);
            }
            // Weapon slot: render sword item
            case "weapon" -> { if (animatable.hasSword()) renderSwordAtBone(poseStack, bone); }
            // Tongue physics
            case "tongue"     -> renderTongueBone(poseStack, bone, false);
            case "mangTongue" -> renderTongueBone(poseStack, bone, true);
            // head3: morning blowjob vertical sway
            case "head3" -> applyMorningBlowjobHeadSway(bone);
            // Eye irises
            case "irisL", "irisR" -> applyIrisOffset(bone, false);
            case "irsisFaceR2", "irsisFaceR3" -> applyIrisOffset(bone, true);
            // RAPE_CHARGE arm IK toward target
            case "armL", "armR" -> applyRapeChargeArmIK(bone);
        }
    }

    // -- Body position interpolation -------------------------------------------

    private Vec3 computeBodyOffset(GalathEntity entity, float partialTick) {
        float phase = entity.getApproachPhase();
        if (phase == -1f) {
            entity.approachStartTick = -1L;
            entity.approachEndTick   = -1L;
            return null;
        }
        var target = entity.getVehicleTarget();
        if (target == null) return null;

        Vec3 targetPos = MathUtil.lerp(
            new Vec3(target.xOld, target.yOld, target.zOld), target.position(), partialTick);

        // Phase 24: fly-to arc (8 ticks)
        if (MathUtil.inRange(phase, 24, 32)) {
            if (entity.approachStartTick == -1L) {
                entity.approachStartTick = Minecraft.getInstance().level.getGameTime();
                entity.approachEndTick   = entity.approachStartTick + 8L;
            }
            Vec3 flyOffset = VectorMathUtil.rotate(new Vec3(0, 0, 3), entity.getYRot() + 180f);
            Vec3 dest = targetPos.add(0, target.getEyeHeight(), 0).add(flyOffset);
            float t = (float)(Minecraft.getInstance().level.getGameTime() + partialTick
                       - entity.approachStartTick) / (float)(entity.approachEndTick - entity.approachStartTick);
            return MathUtil.lerp(entity.getBodyOffset(), dest, t);
        }
        // Phase 32-54: hover behind
        if (MathUtil.inRange(phase, 32, 54)) {
            Vec3 hoverOffset = VectorMathUtil.rotate(new Vec3(0, 0, 1.5), entity.getYRot() + 180f);
            return targetPos.add(hoverOffset);
        }
        return null;
    }

    // -- Yaw helpers -----------------------------------------------------------

    private void applyMasturbateYaw(GalathEntity entity) {
        if (entity.getAnimState() != AnimState.MASTERBATE) return;
        float yaw = entity.getYRot();
        entity.yBodyRot = yaw; entity.yHeadRot = yaw; entity.yHeadRotO = yaw;
        entity.yBodyRotO = yaw; entity.yRotO = yaw;
    }

    private void applyRapeChargeYaw(GalathEntity entity) {
        if (!entity.isKnockOutFlyActive()) return;
        entity.yBodyRot = entity.getYRot(); entity.yBodyRotO = entity.getYRot();
    }

    // -- Hair tilt helpers -----------------------------------------------------

    private void applyHairBackTilt(GeoBone bone) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        var proc = animatable.getAnimProcessor();
        if (proc == null) return;
        var headBone = proc.getBone("head");
        if (headBone == null) return;
        float headRotXDeg = ItemRenderUtil.boneToDegrees(headBone.getRotX());
        if (headRotXDeg < 0f) {
            bone.setRotX(ItemRenderUtil.degreesToBone(-headRotXDeg));
        } else {
            float t = Math.min(1f, headRotXDeg / 45f);
            bone.setRotX(ItemRenderUtil.degreesToBone(-headRotXDeg));
            bone.setPosY(bone.getPosY() + t * 1.5f);
        }
    }

    private void applyHairSideTilt(GeoBone bone) {
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        var proc = animatable.getAnimProcessor();
        if (proc == null) return;
        var headBone = proc.getBone("head");
        if (headBone == null) return;
        float headRotXDeg = ItemRenderUtil.boneToDegrees(headBone.getRotX());
        if (headRotXDeg < 0f) {
            bone.setRotX(ItemRenderUtil.degreesToBone(-headRotXDeg / 2f));
        } else {
            float t = Math.min(1f, headRotXDeg / 45f);
            bone.setRotX(ItemRenderUtil.degreesToBone(-headRotXDeg));
            bone.setPosY(bone.getPosY() + t);
        }
    }

    private void applyHeadTongueTrack(GeoBone bone) {
        var state = animatable.getAnimState();
        if (state == AnimState.FLY || state == AnimState.ATTACK_SWORD) {
            applyFlyAndSwordHeadIK(bone);
        }
    }

    private void applyFlyAndSwordHeadIK(GeoBone bone) {
        var target = animatable.getVehicleTarget();
        if (target == null) return;
        float partialTick = Minecraft.getInstance().getFrameTime();
        Vec3 from = MathUtil.lerp(
            new Vec3(animatable.xOld, animatable.yOld, animatable.zOld),
            animatable.position(), partialTick);
        Vec3 to = MathUtil.lerp(
            new Vec3(target.xOld, target.yOld, target.zOld),
            target.position(), partialTick);
        Vec3 delta = from.subtract(to);
        Vec3 relDelta = VectorMathUtil.rotateByYaw(delta, animatable.getYRot());
        float pitchRad = (float) Math.atan2(delta.y, relDelta.z);
        // Apply as additional head rotation
    }

    // -- Morning-blowjob head sway ---------------------------------------------

    private void applyMorningBlowjobHeadSway(GeoBone bone) {
        if (!AnimState.isInState(animatable, AnimState.MORNING_BLOWJOB_SLOW,
                AnimState.MORNING_BLOWJOB_FAST)) return;
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        float t = Minecraft.getInstance().player.tickCount + Minecraft.getInstance().getFrameTime();
        float swayY = (float)(Math.sin(t * 0.1) * 0.1) + 0.2f;
        float swayZ = (float)Math.sin(t * 0.1) * 0.1f;
        if (AnimState.isInState(animatable, AnimState.MORNING_BLOWJOB_SLOW)) {
            bone.setRotY(bone.getRotY() + swayY);
            bone.setRotZ(bone.getRotZ() + swayZ);
            return;
        }
        if (!animatable.isInSexAnim()) return;
        float factor = 1f - Math.min(0.5f, AnimState.getAnimProgress(animatable,
            Minecraft.getInstance().getFrameTime())) / 0.5f;
        bone.setRotY(bone.getRotY() + swayY * factor);
        bone.setRotZ(bone.getRotZ() + swayZ * factor);
    }

    // -- Iris movement ---------------------------------------------------------

    private void applyIrisOffset(GeoBone bone, boolean isFaceIris) {
        if (!AnimState.isInState(animatable, AnimState.MORNING_BLOWJOB_SLOW)) return;
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        float t = Minecraft.getInstance().player.tickCount + Minecraft.getInstance().getFrameTime();
        float offset = (float)(Math.sin(t * 0.1) * -0.1);
        bone.setPosX((float)(bone.getPosX() + offset));
    }

    // -- RAPE_CHARGE arm IK ----------------------------------------------------

    private void applyRapeChargeArmIK(GeoBone bone) {
        if (animatable.getAnimState() != AnimState.RAPE_CHARGE) return;
        var target = animatable.getVehicleTarget();
        if (target == null) return;
        Vec3 delta = target.position().subtract(animatable.position());
        Vec3 rotated = VectorMathUtil.rotateByYaw(delta, animatable.getYRot());
        double armX = MathUtil.clamp(rotated.x, -1.0, 1.0);
        bone.setRotZ((float)(bone.getRotZ() + ItemRenderUtil.degreesToBone(45.0 * armX)));
    }

    // -- Tongue/tentacle rendering ---------------------------------------------

    private void renderTongueBone(PoseStack poseStack, GeoBone bone, boolean isMangTongue) {
        boolean shouldRender = isMangTongue
            ? AnimState.isInState(animatable, AnimState.MORNING_BLOWJOB_SLOW)
                || animatable.isMangLieActive()
            : AnimState.isInState(animatable, AnimState.PUSSY_LICKING,
                AnimState.MASTERBATE_SITTING, AnimState.MORNING_BLOWJOB_SLOW);
        if (!shouldRender) return;
        // TODO: push poseStack to bone pivot and render PhysicsParticleChain
    }

    // -- Sword item render -----------------------------------------------------

    private void renderSwordAtBone(PoseStack poseStack, GeoBone bone) {
        // TODO: push matrix to bone, render sword item stack via ItemRenderer
    }

    // -- Post-super: hair strand + effects ------------------------------------

    private void renderHairAndEffects(GalathEntity entity, PoseStack poseStack,
                                       MultiBufferSource bufferSource, float partialTick) {
        if (entity.getAnimState() == AnimState.GIVE_COIN
                && AnimState.getTicksPlaying(entity)[1] > 100) return;

        // Hair strand physics (right and left)
        PhysicsParticleRenderer.renderHairStrands(entity, bufferSource, poseStack, partialTick,
            "hairStrandStartR", "hairStrandMidR", "hairStrandEndR", TONGUE_SETTLED);
        PhysicsParticleRenderer.renderHairStrands(entity, bufferSource, poseStack, partialTick,
            "hairStrandStartL", "hairStrandMidL", "hairStrandEndL", TONGUE_SETTLED);

        // Star ring / pentagram when player is bound
        if (entity.hasPlayerBound()) {
            PhysicsParticleRenderer.renderGalathStarRing(entity, bufferSource, poseStack, partialTick);
        }
    }

    // -- IBoneFilter -----------------------------------------------------------

    @Override
    public boolean isBoneVisible(GalathEntity entity, String boneName) {
        return !HIDDEN_BONES.contains(boneName);
    }

    // -- Static inner ---------------------------------------------------------

    static class HiddenBoneSet extends HashSet<String> {
        HiddenBoneSet() {
            add("static"); add("turnable"); add("slip");
            add("boobs"); add("booty"); add("vagina"); add("fuckhole");
            add("futaBallLR"); add("futaBallLL"); add("coin"); add("pentagram");
        }
    }

    static {
        TONGUE_CONFIG_G = new PhysicsParticleChain.Config(
            TONGUE_COLOR, 0.1f, 12, 0.035f,
            (i, t) -> (float)(Math.sin(t * 0.3 + -0.2 * i) * 15),
            (i, t) -> (float)(Math.sin(t * -0.15 + -0.2 * i) * 3),
            (i, t) -> 0f,
            0.03f, 0.005f);

        TONGUE_CONFIG_T = new PhysicsParticleChain.Config(
            TONGUE_COLOR, 0.0f, 12, 0.0f,
            (i, t) -> (float)(Math.sin(t * 0.3 + -0.2 * i) * 15),
            (i, t) -> (float)(Math.sin(t * -0.15 + -0.2 * i) * 3),
            (i, t) -> 0f,
            0.03f, 0.005f);
    }
}
