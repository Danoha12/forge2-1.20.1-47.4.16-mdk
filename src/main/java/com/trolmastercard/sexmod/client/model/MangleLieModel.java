package com.trolmastercard.sexmod.client.model;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.client.ClientProxy;
import com.trolmastercard.sexmod.client.FakeWorld;
import com.trolmastercard.sexmod.entity.*;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.NpcRenderUtil;
import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.util.YawPitch;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * MangleLieModel - ported from ce.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * GeoModel for the MangleLie entity. Three geo files:
 *   [0] = manglelie.geo.json  (default + standing)
 *   [1] = manglelie.geo.json  (same)
 *   [2] = galath_con_mang.geo.json  (threesome combined model)
 *
 * Contains complex arm IK: when the MangleLie is grabbing a player or floating,
 * it computes a per-arm ArmResult (struct: upper/lower arm angles, elbow yaw, scales)
 * and lerps between the "free" and "grab" state.
 *
 * Additional per-frame logic:
 *   b(entity) - updateHeadLook:   rotate head toward the sex partner
 *   d(entity) - updateArmGrab:    compute arm IK toward grabbed player
 *   a(entity) - updateBodyRotY:   mirror Galath's body rotation into MangleLie's body
 *   e(entity) - updateLegArmor:   adjust leg/arm bones when Galath is in specific poses
 *
 * Static helpers (called from GalathModel.applyGalathBones):
 *   a(em, AnimationProcessor, float) - applyGalathBones: toggles skirt/cock stage bones
 *   b(em, AnimationProcessor, float) - applyCockStageBones
 *   e(AnimationProcessor, bool) - showBelowSkirtBones
 *   f(AnimationProcessor, bool) - showSkirtBones
 *
 * GeckoLib 3 - 4:
 *   IBone - CoreGeoBone, setRotationX/Y/Z - setRotX/Y/Z, setPositionX/Y/Z - setPosX/Y/Z
 *   AnimationState.Transitioning - check via controller
 *   javax.vecmath.Vector3f - org.joml.Vector3f (not needed here - full Vec3 used)
 *
 * Field constants:
 *   ce.h = 7.0F   (MAX_HEAD_ANGLE)
 *   ce.k = 0.75F  (HEAD_PITCH_CLAMP)
 *   ce.l = gc.c(140.0F) = toRad(140)  (LOWER_ARM_BASE)
 *   ce.m = gc.c(35.0F)  = toRad(35)   (UPPER_ARM_BASE)
 *   ce.i = 90.0F  (ARM_BASE_DEGREES)
 *   ce.g = gc.c(45.0F)  = toRad(45)   (ELBOW_ANGLE_A)
 *   ce.f = gc.c(-45.0F) = toRad(-45)  (ELBOW_ANGLE_B)
 */
public class MangleLieModel extends BaseNpcModel<BaseNpcEntity> {

    static final float MAX_HEAD_ANGLE    = 7.0F;
    static final float HEAD_PITCH_CLAMP  = 0.75F;
    static final float LOWER_ARM_BASE    = MathUtil.toRad(140.0F);
    static final float UPPER_ARM_BASE    = MathUtil.toRad(35.0F);
    static final float ARM_BASE_DEGREES  = 90.0F;
    static final float ELBOW_ANGLE_A     = MathUtil.toRad(45.0F);
    static final float ELBOW_ANGLE_B     = MathUtil.toRad(-45.0F);

    public static final ResourceLocation TEXTURE =
        new ResourceLocation("sexmod", "textures/entity/manglelie/manglelie.png");

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
            new ResourceLocation("sexmod", "geo/manglelie/manglelie.geo.json"),
            new ResourceLocation("sexmod", "geo/manglelie/manglelie.geo.json"),
            new ResourceLocation("sexmod", "geo/galath/galath_con_mang.geo.json")
        };
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        if (entity.level() instanceof FakeWorld) return c[0];
        if (isThreesome(entity)) return c[2];
        return c[entity.getEntityData().get(BaseNpcEntity.MODEL_INDEX)];
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/manglelie/manglelie.animation.json");
    }

    public static boolean isThreesome(BaseNpcEntity entity) {
        return AnimState.isAnyOf(entity.getAnimState(),
            AnimState.THREESOME_SLOW, AnimState.THREESOME_FAST, AnimState.THREESOME_CUM);
    }

    // =========================================================================
    //  setCustomAnimations
    // =========================================================================

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId,
                                    AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);
        applyGalathBones(entity, getAnimationProcessor(), animState.getPartialTick());
        updateHeadLook(entity);
        updateArmGrab(entity, animState.getPartialTick());
        updateBodyRotY(entity);
        updateLegArmor(entity);
    }

    // =========================================================================
    //  updateLegArmor (original: ce.e(em))
    // =========================================================================

    void updateLegArmor(BaseNpcEntity entity) {
        if (Minecraft.getInstance().isWindowActive()) return;
        if (isThreesome(entity)) return;

        GalathEntity galath = MangleLieEntity.getGalath(entity, false);
        if (galath == null) return;

        if (!AnimState.isAnyOf(galath.getAnimState(),
            AnimState.CORRUPT_CUM, AnimState.CARRY_FAST, AnimState.CORRUPT_INTRO, AnimState.CORRUPT_SLOW))
            return;

        var proc = getAnimationProcessor();
        CoreGeoBone legR     = proc.getBone("legR");
        CoreGeoBone lowerArmR = proc.getBone("lowerArmR");
        CoreGeoBone lowerArmL = proc.getBone("lowerArmL");
        if (legR != null) legR.setRotY(legR.getRotY() + ELBOW_ANGLE_B);
        if (lowerArmR != null) lowerArmR.setRotX(lowerArmR.getRotX() + ELBOW_ANGLE_B);
        if (lowerArmL != null) lowerArmL.setRotX(lowerArmL.getRotX() + ELBOW_ANGLE_B);
    }

    // =========================================================================
    //  updateBodyRotY (original: ce.a(em) - applies Galath body rot to MangleLie)
    // =========================================================================

    void updateBodyRotY(BaseNpcEntity entity) {
        if (!(entity instanceof MangleLieEntity mangle)) return;
        if (isThreesome(entity)) return;

        GalathEntity galath = mangle.getGalath(false);
        if (galath == null) return;

        CoreGeoBone body = getAnimationProcessor().getBone("body");
        if (body == null) return;

        boolean transitioning = false; // simplified  check controller state
        body.setRotY(galath.bodyRotY + (transitioning ? 0.0F : body.getRotY()));
        body.setScaleX(galath.bodyScaleY);
        body.setScaleY(galath.bodyScaleY);
        body.setScaleZ(galath.bodyScaleY);
    }

    // =========================================================================
    //  updateHeadLook (original: ce.b(em))
    // =========================================================================

    void updateHeadLook(BaseNpcEntity entity) {
        if (ClientProxy.IS_PRELOADING) return;
        if (Minecraft.getInstance().isWindowActive()) return;
        if (!(entity instanceof MangleLieEntity mangle)) return;
        if (!com.trolmastercard.sexmod.util.NpcRenderUtil.isSexDoggyActive(mangle)) return;

        GalathEntity galath = mangle.getGalath(false);
        if (galath == null) return;

        var proc = getAnimationProcessor();
        float galathHeadRotX = galath.headRotX;
        proc.getBone("rotationTool").setRotX(galathHeadRotX);

        CoreGeoBone head  = proc.getBone("head");
        CoreGeoBone upper = proc.getBone("upperBody");
        CoreGeoBone boobs = proc.getBone("boobs");

        if (galathHeadRotX > 0.0F) {
            if (upper != null) upper.setRotX(-1.1111112F * galathHeadRotX);
            if (head  != null) head.setRotX(0.1333F * galathHeadRotX);
            if (boobs != null) boobs.setRotX(galathHeadRotX * 22.5F / 45.0F);
        } else {
            if (upper != null) upper.setRotX(-1.6666666F * galathHeadRotX);
            if (head  != null) head.setRotX(galathHeadRotX * 0.666F);
        }

        // Head yaw tracking
        float partial = Minecraft.getInstance().getPartialTick();
        float dt1 = NpcRenderUtil.angleDeltaSmooth(mangle.headYaw, mangle.prevHeadYaw);
        float dt2 = NpcRenderUtil.angleDeltaSmooth(mangle.headPitch, mangle.prevHeadPitch);
        float fps = Minecraft.getInstance().getFps();
        if (fps == 0.0F) fps = 1.0F;

        float v1 = MAX_HEAD_ANGLE * MathUtil.clamp(dt1, -MAX_HEAD_ANGLE, MAX_HEAD_ANGLE) / fps;
        float v2 = MAX_HEAD_ANGLE * MathUtil.clamp(dt2, -MAX_HEAD_ANGLE, MAX_HEAD_ANGLE) / fps;
        float hy = mangle.headYaw  + v1;
        float hp = mangle.headPitch + v2;

        if (head != null) {
            head.setRotY(head.getRotY() + hy);
            head.setRotX(head.getRotX() + hp);
        }
        mangle.headYaw   = hy;
        mangle.headPitch = hp;
    }

    // =========================================================================
    //  updateArmGrab (original: ce.d(em)) - full arm IK
    // =========================================================================

    void updateArmGrab(BaseNpcEntity entity, float partial) {
        if (ClientProxy.IS_PRELOADING) return;
        if (isThreesome(entity)) return;
        if (Minecraft.getInstance().isWindowActive()) return;
        if (!(entity instanceof MangleLieEntity mangle)) return;
        if (!mangle.isGrabbing()) return;

        GalathEntity galath = mangle.getGalath(false);
        if (galath == null) return;

        var proc = getAnimationProcessor();
        CoreGeoBone armL     = proc.getBone("armL");
        CoreGeoBone armR     = proc.getBone("armR");
        CoreGeoBone lowerArmL = proc.getBone("lowerArmL");
        CoreGeoBone lowerArmR = proc.getBone("lowerArmR");
        CoreGeoBone elbowR   = proc.getBone("elbowR");
        CoreGeoBone elbowL   = proc.getBone("elbowL");

        Entity grabbed = mangle.getGrabbedEntity();
        boolean noTarget = (grabbed == null);
        if (!noTarget) mangle.grabTargetPos = getEntityCenter(grabbed);

        float fps = Minecraft.getInstance().getFps();
        if (fps == 0.0F) fps = 1.0F;

        if (mangle.noTargetPrev == noTarget) {
            mangle.grabTransition = 0.0F;
        } else {
            mangle.grabTransition += 1.5F / fps;
        }
        if (mangle.grabTransition >= 1.0F) {
            mangle.grabTransition = 0.0F;
            mangle.noTargetPrev = noTarget;
        }

        ArmResult result;
        if (mangle.grabTransition == 0.0F) {
            result = noTarget
                ? computeArmsFree(galath, armR, armL, lowerArmR, lowerArmL)
                : computeArmsGrab(mangle, galath, lowerArmR, lowerArmL, proc);
        } else {
            float t = (float) (mangle.noTargetPrev
                ? MathUtil.easeInOut(mangle.grabTransition)
                : 1.0D - MathUtil.easeInOut(mangle.grabTransition));
            result = ArmResult.lerp(
                computeArmsFree(galath, armR, armL, lowerArmR, lowerArmL),
                computeArmsGrab(mangle, galath, lowerArmR, lowerArmL, proc),
                t);
        }

        if (armR    != null) { armR.setRotX(result.armR.r);    armR.setRotY(result.armR.g);    armR.setRotZ(result.armR.b); }
        if (armL    != null) { armL.setRotX(result.armL.r);    armL.setRotY(result.armL.g);    armL.setRotZ(result.armL.b); }
        if (lowerArmL!=null) { lowerArmL.setRotX(result.lowerArmL.r); lowerArmL.setRotY(result.lowerArmL.g); lowerArmL.setRotZ(result.lowerArmL.b); }
        if (lowerArmR!=null) { lowerArmR.setRotX(result.lowerArmR.r); lowerArmR.setRotY(result.lowerArmR.g); lowerArmR.setRotZ(result.lowerArmR.b); }
        if (armL    != null) armL.setScaleY(result.scaleArmL);
        if (armR    != null) armR.setScaleY(result.scaleArmR);
        if (elbowR  != null) elbowR.setRotY(result.elbowRot);
        if (elbowL  != null) elbowL.setRotY(result.elbowRotL);
    }

    // ---- Arm computation helpers (original: ce.a/a overloads) -----

    private ArmResult computeArmsFree(@Nonnull GalathEntity galath,
            CoreGeoBone armR, CoreGeoBone armL, CoreGeoBone lowerArmR, CoreGeoBone lowerArmL) {
        ArmResult r = new ArmResult();
        float aE = galath.headRotX;
        if (aE > 0.0F) {
            r.armR     = new RgbColor(armR.getRotX() - aE,    armR.getRotZ() + aE * 12.5F / 45.0F,   armR.getRotY() - aE * -25.0F / 45.0F);
            r.armL     = new RgbColor(armL.getRotX() - aE,    armL.getRotZ(),                          armL.getRotY() + aE * 15.0F / 45.0F);
            r.lowerArmL = new RgbColor(lowerArmL != null ? lowerArmL.getRotX() : 0, 0, lowerArmL != null ? lowerArmL.getRotZ() : 0);
            r.lowerArmR = new RgbColor(lowerArmR != null ? lowerArmR.getRotX() : 0, 0, lowerArmR != null ? lowerArmR.getRotZ() : 0);
        } else {
            r.lowerArmR = new RgbColor((lowerArmR != null ? lowerArmR.getRotX() : 0) + 2.0F * aE, 0, lowerArmR != null ? lowerArmR.getRotZ() : 0);
            r.lowerArmL = new RgbColor((lowerArmL != null ? lowerArmL.getRotX() : 0) + 2.2222223F * aE, 0, lowerArmL != null ? lowerArmL.getRotZ() : 0);
            r.armR      = new RgbColor((armR != null ? armR.getRotX() : 0) - aE, (armR != null ? armR.getRotZ() : 0) + aE * 5.0F / 45.0F, armR != null ? armR.getRotY() : 0);
            r.armL      = new RgbColor((armL != null ? armL.getRotX() : 0) - aE, (armL != null ? armL.getRotZ() : 0) - aE * 5.0F / 45.0F, armL != null ? armL.getRotY() : 0);
        }
        return r;
    }

    private ArmResult computeArmsGrab(@Nonnull MangleLieEntity mangle, @Nonnull GalathEntity galath,
            CoreGeoBone lowerArmR, CoreGeoBone lowerArmL, AnimationProcessor proc) {
        ArmResult r = new ArmResult();
        r.lowerArmL = new RgbColor(UPPER_ARM_BASE, lowerArmL != null ? lowerArmL.getRotZ() : 0, 0);
        r.lowerArmR = new RgbColor(LOWER_ARM_BASE, lowerArmR != null ? lowerArmR.getRotZ() : 0, 0);

        float partial = Minecraft.getInstance().getPartialTick();
        Vec3 galathPos = com.trolmastercard.sexmod.util.NpcRenderUtil.getInterpolatedPos(galath, partial);
        Vec3 armRPos   = mangle.getBoneWorldPos("armR").subtract(galathPos);
        Vec3 armLPos   = mangle.getBoneWorldPos("armL").subtract(galathPos);
        YawPitch bm1   = NpcRenderUtil.computeArmAngles(armRPos, mangle.grabTargetPos);
        YawPitch bm2   = NpcRenderUtil.computeArmAngles(armLPos, mangle.grabTargetPos);

        Float galathYaw = galath.getSexTargetYaw(galath, partial);
        float yawF = (galathYaw == null)
            ? MathUtil.lerpYaw(galath.yBodyRot, galath.yBodyRotO, partial)
            : galathYaw;
        float yawR = MathUtil.toRad(yawF);

        float reach = mangle.getReachProgress(partial);
        float reachEased = (float) MathUtil.easeInOut(Math.min(1.0F, reach));
        float armExtend  = reachEased != 1.0F ? 0.0F : (reach * 28.0F - 28.0F) / 32.0F;
        armExtend = Math.max(0.0F, armExtend - 0.5F) * 2.0F;
        float armEased  = (float) MathUtil.easeInOut(armExtend);
        float grip = MathUtil.toRad(MathUtil.lerp(0.0F, 90.0F, reachEased));

        boolean rightSide = mangle.isRightSideGrab(mangle.grabTargetPos, partial);
        if (rightSide) {
            r.armR  = new RgbColor(-galath.headRotX + bm1.yaw + MathUtil.toRad(90.0F), 0, bm1.pitch);
            r.armL  = new RgbColor(-galath.headRotX + bm2.yaw + MathUtil.toRad(90.0F),
                (float)(bm2.pitch + MathUtil.toRad(-20.0F) * Math.cos(bm1.pitch + yawR)
                    + MathUtil.lerp(grip / 2.0F, 0.0F, armEased)), 0);
            r.scaleArmL = 1.0F + Math.abs(Math.abs(bm1.pitch) - Math.abs(yawR)) * 0.1909F;
            r.elbowRotL = MathUtil.toRad(90.0F);
            r.lowerArmL.b = MathUtil.lerp(grip, 0.0F, armEased);
            if (armExtend > 0.5D) {
                r.lowerArmL.r = UPPER_ARM_BASE + (float) MathUtil.lerp(ELBOW_ANGLE_A, 0.0D,
                    MathUtil.easeInOut((armExtend - 0.5F) * 2.0F));
            }
            r.armR.g  += yawR;
            r.armL.g  += yawR;
        } else {
            r.armL  = new RgbColor(-galath.headRotX + bm2.yaw + MathUtil.toRad(90.0F), 0, bm2.pitch);
            r.armR  = new RgbColor(-galath.headRotX + bm1.yaw + MathUtil.toRad(90.0F),
                (float)(bm1.pitch + MathUtil.toRad(20.0F) * Math.cos(bm2.pitch + yawR)
                    - MathUtil.lerp(grip / 2.0F, 0.0F, armEased)), 0);
            r.scaleArmR = 1.0F + Math.abs(Math.abs(bm2.pitch) - Math.abs(yawR)) * 0.1909F;
            r.elbowRot = MathUtil.toRad(90.0F);
            r.lowerArmR.b = -MathUtil.lerp(grip, 0.0F, armEased);
            if (armExtend > 0.5D) {
                r.lowerArmR.r = LOWER_ARM_BASE + (float) MathUtil.lerp(ELBOW_ANGLE_A, 0.0D,
                    MathUtil.easeInOut((armExtend - 0.5F) * 2.0F));
            }
            r.armR.g  += yawR;
            r.armL.g  += yawR;
        }
        return r;
    }

    private Vec3 getEntityCenter(@Nonnull Entity entity) {
        return com.trolmastercard.sexmod.util.NpcRenderUtil.getInterpolatedPos(entity,
            Minecraft.getInstance().getPartialTick())
            .add(0, entity.getEyeHeight(), 0);
    }

    // =========================================================================
    //  Static helpers for GalathModel
    // =========================================================================

    /**
     * Called from GalathModel during setCustomAnimations when Galath is in
     * masturbate mode. Toggles skirt/cock-stage bones.
     * Original: {@code ce.a(em, AnimationProcessor, float)}
     */
    public static void applyGalathBones(BaseNpcEntity entity,
                                        AnimationProcessor proc, float partial) {
        if (ClientProxy.IS_PRELOADING) return;
        boolean hasSkirt = NpcRenderUtil.hasSkirt(entity);
        showBelowSkirtBones(proc, hasSkirt);
        showSkirtBones(proc, hasSkirt);
        applyCockStageBones(entity, proc, partial);
    }

    static void applyCockStageBones(BaseNpcEntity entity, AnimationProcessor proc, float partial) {
        if (!(entity instanceof MangleLieEntity mangle)) return;
        for (int i = 0; i < 3; i++) {
            CoreGeoBone bone = proc.getBone("cockStage" + i);
            if (bone != null) bone.setHidden(i > mangle.cockStage);
        }
    }

    static void showSkirtBones(AnimationProcessor proc, boolean hasSkirt) {
        CoreGeoBone b = proc.getBone("skirt");
        if (b != null) b.setHidden(!hasSkirt);
    }

    static void showBelowSkirtBones(AnimationProcessor proc, boolean hasSkirt) {
        for (String name : new String[]{ "cheekRBelowSkirt", "cheekLBelowSkirt",
                "sideRNoSkirt", "sideLNoSkirt" }) {
            CoreGeoBone b = proc.getBone(name);
            if (b != null) b.setHidden(hasSkirt);
        }
        CoreGeoBone sr = proc.getBone("sideRSkirt");
        if (sr != null) sr.setHidden(!hasSkirt);
        CoreGeoBone sl = proc.getBone("sideLSkirt");
        if (sl != null) sl.setHidden(!hasSkirt);
    }

    /**
     * Computes body orientation vec for KNOCK_OUT_FLY / RAPE_CHARGE states.
     * Original: {@code cb.d(em)} (in GalathModel but logically part of this system).
     */
    public static Vec3 computeBodyVec(BaseNpcEntity entity, float partial) {
        // Compute direction entity is moving and convert to body tilt
        Vec3 vel = entity.getDeltaMovement();
        float angle = (float) Math.atan2(vel.z, vel.x);
        float pitch = (float) Math.atan2(vel.y, Math.sqrt(vel.x * vel.x + vel.z * vel.z));
        float yawOff = (float) entity.getYRot();
        return new Vec3(pitch, vel.y * 2.0, Math.sin(angle - Math.toRadians(yawOff)) * 0.5);
    }

    // =========================================================================
    //  Slot bone arrays
    // =========================================================================

    @Override public String[] getHelmetBones()    { return new String[]{ "armorHelmet" }; }

    // =========================================================================
    //  ArmResult inner struct  (original: ce.a inner class)
    // =========================================================================

    static class ArmResult {
        RgbColor armR     = new RgbColor(0, 0, 0);
        RgbColor armL     = new RgbColor(0, 0, 0);
        RgbColor lowerArmR = new RgbColor(0, 0, 0);
        RgbColor lowerArmL = new RgbColor(0, 0, 0);
        float scaleArmR   = 1.0F;
        float scaleArmL   = 1.0F;
        float elbowRot    = 0.0F;
        float elbowRotL   = 0.0F;

        static ArmResult lerp(ArmResult a, ArmResult b, float t) {
            ArmResult r  = new ArmResult();
            r.armR       = RgbColor.lerp(a.armR,       b.armR,       t);
            r.armL       = RgbColor.lerp(a.armL,       b.armL,       t);
            r.lowerArmR  = RgbColor.lerp(a.lowerArmR,  b.lowerArmR,  t);
            r.lowerArmL  = RgbColor.lerp(a.lowerArmL,  b.lowerArmL,  t);
            r.scaleArmR  = MathUtil.lerp(a.scaleArmR,  b.scaleArmR,  t);
            r.scaleArmL  = MathUtil.lerp(a.scaleArmL,  b.scaleArmL,  t);
            r.elbowRot   = MathUtil.lerp(a.elbowRot,   b.elbowRot,   t);
            r.elbowRotL  = MathUtil.lerp(a.elbowRotL,  b.elbowRotL,  t);
            return r;
        }
    }
}
