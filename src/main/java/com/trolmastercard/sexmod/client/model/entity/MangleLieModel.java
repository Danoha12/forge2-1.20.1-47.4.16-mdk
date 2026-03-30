package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.client.ClientProxy;
import com.trolmastercard.sexmod.client.FakeWorld;
import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.NpcRenderUtil;
import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.util.YawPitch;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * MangleLieModel — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja IK complejo para los brazos (agarre de jugador).
 * * Sincroniza rotación y escala con GalathEntity.
 * * Gestiona modelos combinados para escenas de trío.
 */
public class MangleLieModel extends BaseNpcModel<BaseNpcEntity> {

    static final float MAX_HEAD_ANGLE = 7.0F;
    static final float LOWER_ARM_BASE = (float) Math.toRadians(140.0F);
    static final float UPPER_ARM_BASE = (float) Math.toRadians(35.0F);
    static final float ELBOW_ANGLE_A = (float) Math.toRadians(45.0F);
    static final float ELBOW_ANGLE_B = (float) Math.toRadians(-45.0F);

    public static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/entity/manglelie/manglelie.png");

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
        ResourceLocation[] geos = getGeoFiles();
        if (entity.level() instanceof FakeWorld) return geos[0];
        if (isThreesome(entity)) return geos[2];

        int idx = entity.getEntityData().get(BaseNpcEntity.DATA_OUTFIT_INDEX);
        return (idx >= 0 && idx < geos.length) ? geos[idx] : geos[0];
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/manglelie/manglelie.animation.json");
    }

    private static boolean isThreesome(BaseNpcEntity entity) {
        return AnimState.anyOf(entity.getAnimState(), AnimState.THREESOME_SLOW, AnimState.THREESOME_FAST, AnimState.THREESOME_CUM);
    }

    // ── Lógica de Animación Procedimental ───────────────────────────────────

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId, AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        float partialTick = animState.getPartialTick();
        AnimationProcessor<BaseNpcEntity> proc = getAnimationProcessor();

        // 1. Sincronización con Galath (Mommy)
        applyGalathBones(entity, proc, partialTick);
        updateBodySync(entity, proc);

        // 2. Lógica de Cabeza y Brazos (IK)
        updateHeadLook(entity, proc);
        updateArmGrabIK(entity, proc, partialTick);
        updateLegArmor(entity, proc);
    }

    private void updateBodySync(BaseNpcEntity entity, AnimationProcessor<BaseNpcEntity> proc) {
        if (!(entity instanceof MangleLieEntity mangle) || isThreesome(entity)) return;

        GalathEntity galath = mangle.getMommy(false);
        if (galath == null) return;

        CoreGeoBone body = proc.getBone("body");
        if (body != null) {
            // Sincronizamos rotación Y y escala de Galath hacia Manglelie
            body.setRotY(galath.bodyRotY);
            body.setScaleX(galath.bodyScaleY);
            body.setScaleY(galath.bodyScaleY);
            body.setScaleZ(galath.bodyScaleY);
        }
    }

    private void updateHeadLook(BaseNpcEntity entity, AnimationProcessor<BaseNpcEntity> proc) {
        if (!(entity instanceof MangleLieEntity mangle) || Minecraft.getInstance().isWindowActive()) return;

        GalathEntity galath = mangle.getMommy(false);
        if (galath == null) return;

        float galathHeadX = galath.headRotX;
        CoreGeoBone rotTool = proc.getBone("rotationTool");
        if (rotTool != null) rotTool.setRotX(galathHeadX);

        CoreGeoBone head = proc.getBone("head");
        CoreGeoBone upper = proc.getBone("upperBody");

        if (head != null && upper != null) {
            if (galathHeadX > 0) {
                upper.setRotX(-1.11F * galathHeadX);
                head.setRotX(0.13F * galathHeadX);
            } else {
                upper.setRotX(-1.66F * galathHeadX);
                head.setRotX(galathHeadX * 0.66F);
            }

            // Seguimiento suave de la mirada
            head.setRotY(head.getRotY() + mangle.headYaw);
            head.setRotX(head.getRotX() + mangle.headPitch);
        }
    }

    private void updateArmGrabIK(BaseNpcEntity entity, AnimationProcessor<BaseNpcEntity> proc, float partial) {
        if (!(entity instanceof MangleLieEntity mangle) || isThreesome(entity)) return;
        if (!mangle.isSexModeActive()) return; // Reemplazo de isGrabbing

        GalathEntity galath = mangle.getMommy(false);
        if (galath == null) return;

        Entity grabbed = mangle.getTargetEntity();
        boolean hasTarget = (grabbed != null);

        // Transición de suavizado para el agarre
        float fps = Math.max(1.0F, Minecraft.getInstance().getFps());
        if (mangle.hasTarget == hasTarget) {
            mangle.rideRotY = 0.0F; // Usamos rideRotY como acumulador de transición
        } else {
            mangle.rideRotY += 1.5F / fps;
        }

        if (mangle.rideRotY >= 1.0F) {
            mangle.rideRotY = 0.0F;
            mangle.hasTarget = hasTarget;
        }

        ArmResult free = computeArmsFree(galath, proc);
        ArmResult grab = computeArmsGrab(mangle, galath, proc, partial);

        float t = mangle.hasTarget ? 1.0F - MathUtil.easeInOut(mangle.rideRotY) : MathUtil.easeInOut(mangle.rideRotY);
        ArmResult finalResult = ArmResult.lerp(free, grab, t);

        applyArmResult(proc, finalResult);
    }

    private ArmResult computeArmsFree(GalathEntity galath, AnimationProcessor<BaseNpcEntity> proc) {
        ArmResult r = new ArmResult();
        float headX = galath.headRotX;

        CoreGeoBone armR = proc.getBone("armR");
        CoreGeoBone armL = proc.getBone("armL");

        if (armR != null && armL != null) {
            if (headX > 0) {
                r.rotR = new Vec3(armR.getRotX() - headX, armR.getRotY() + headX * 0.27, armR.getRotZ());
                r.rotL = new Vec3(armL.getRotX() - headX, armL.getRotY(), armL.getRotZ() + headX * 0.33);
            } else {
                r.rotR = new Vec3(armR.getRotX() - headX, armR.getRotY() + headX * 0.11, armR.getRotZ());
                r.rotL = new Vec3(armL.getRotX() - headX, armL.getRotY() - headX * 0.11, armL.getRotZ());
            }
        }
        return r;
    }

    private ArmResult computeArmsGrab(MangleLieEntity mangle, GalathEntity galath, AnimationProcessor<BaseNpcEntity> proc, float partial) {
        ArmResult r = new ArmResult();
        r.rotLowerL = new Vec3(UPPER_ARM_BASE, 0, 0);
        r.rotLowerR = new Vec3(LOWER_ARM_BASE, 0, 0);

        Entity target = mangle.getTargetEntity();
        if (target == null) return r;

        Vec3 targetPos = target.position().add(0, target.getEyeHeight(), 0);
        Vec3 armRPos = mangle.getBoneWorldPos("armR");

        YawPitch angles = NpcRenderUtil.computeArmAngles(armRPos, targetPos);
        float headX = galath.headRotX;

        // Matemáticas de rotación IK
        r.rotR = new Vec3(-headX + angles.yaw + Math.toRadians(90), 0, angles.pitch);
        r.rotL = new Vec3(-headX + angles.yaw + Math.toRadians(90), Math.toRadians(-20), 0);

        return r;
    }

    private void applyArmResult(AnimationProcessor<BaseNpcEntity> proc, ArmResult res) {
        CoreGeoBone armR = proc.getBone("armR");
        CoreGeoBone armL = proc.getBone("armL");
        if (armR != null) armR.updateRotation((float)res.rotR.x, (float)res.rotR.y, (float)res.rotR.z);
        if (armL != null) armL.updateRotation((float)res.rotL.x, (float)res.rotL.y, (float)res.rotL.z);
    }

    private void updateLegArmor(BaseNpcEntity entity, AnimationProcessor<BaseNpcEntity> proc) {
        if (!(entity instanceof MangleLieEntity mangle)) return;
        GalathEntity galath = mangle.getMommy(false);
        if (galath == null) return;

        if (AnimState.anyOf(galath.getAnimState(), AnimState.CORRUPT_CUM, AnimState.CORRUPT_SLOW)) {
            CoreGeoBone legR = proc.getBone("legR");
            if (legR != null) legR.setRotY(legR.getRotY() + ELBOW_ANGLE_B);
        }
    }

    // ── Helpers Estáticos (Invocados por GalathModel) ─────────────────────────

    public static void applyGalathBones(BaseNpcEntity entity, AnimationProcessor<BaseNpcEntity> proc, float partial) {
        if (ClientProxy.IS_PRELOADING) return;
        boolean hasSkirt = NpcRenderUtil.hasSkirt(entity);

        setBoneVisible(proc, "skirt", hasSkirt);
        setBoneVisible(proc, "cheekRBelowSkirt", !hasSkirt);
        setBoneVisible(proc, "cheekLBelowSkirt", !hasSkirt);

        if (entity instanceof MangleLieEntity mangle) {
            for (int i = 0; i < 3; i++) {
                setBoneVisible(proc, "cockStage" + i, i <= mangle.cumStageIndex);
            }
        }
    }

    private static void setBoneVisible(AnimationProcessor<?> proc, String name, boolean visible) {
        CoreGeoBone bone = proc.getBone(name);
        if (bone != null) bone.setHidden(!visible);
    }

    public static Vec3 computeBodyVec(BaseNpcEntity entity, float partial) {
        Vec3 vel = entity.getDeltaMovement();
        double horizontalMag = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        float pitch = (float) Math.atan2(vel.y, horizontalMag);
        float yaw = (float) Math.toRadians(entity.getYRot());

        return new Vec3(pitch, vel.y * 2.0, Math.sin(Math.atan2(vel.z, vel.x) - yaw) * 0.5);
    }

    @Override public String[] getHelmetBones() { return new String[]{ "armorHelmet" }; }

    // ── Estructura de Resultado IK ───────────────────────────────────────────

    private static class ArmResult {
        Vec3 rotR = Vec3.ZERO;
        Vec3 rotL = Vec3.ZERO;
        Vec3 rotLowerR = Vec3.ZERO;
        Vec3 rotLowerL = Vec3.ZERO;

        static ArmResult lerp(ArmResult a, ArmResult b, float t) {
            ArmResult res = new ArmResult();
            res.rotR = MathUtil.lerpVec3(a.rotR, b.rotR, t);
            res.rotL = MathUtil.lerpVec3(a.rotL, b.rotL, t);
            res.rotLowerR = MathUtil.lerpVec3(a.rotLowerR, b.rotLowerR, t);
            res.rotLowerL = MathUtil.lerpVec3(a.rotLowerL, b.rotLowerL, t);
            return res;
        }
    }
}