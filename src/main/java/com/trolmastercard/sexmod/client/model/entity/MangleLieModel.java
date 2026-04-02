package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.client.model.BaseNpcModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.util.MathUtil;
// import com.trolmastercard.sexmod.util.NpcRenderUtil; // Comentado por seguridad si no existe
// import com.trolmastercard.sexmod.util.YawPitch; // Comentado por seguridad
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// 🚨 CORREGIDO: Import de GeoBone actualizado a GeckoLib 4
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

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
        // 🚨 CORREGIDO: Eliminamos el FakeWorld
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
        AnimState st = entity.getAnimState();
        // 🚨 CORREGIDO: Quitamos el anyOf conflictivo
        return (st == AnimState.THREESOME_SLOW || st == AnimState.THREESOME_FAST || st == AnimState.THREESOME_CUM);
    }

    // ── Lógica de Animación Procedimental ───────────────────────────────────

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId, AnimationState<BaseNpcEntity> animState) {
        super.setCustomAnimations(entity, instanceId, animState);

        // 🚨 CORREGIDO: getPartialTick a getFrameTime
        float partialTick = Minecraft.getInstance().getFrameTime();
        AnimationProcessor<BaseNpcEntity> proc = getAnimationProcessor();

        // 1. Sincronización con Galath
        // applyGalathBones(entity, proc, partialTick); // Comentado temporalmente por error estático
        // updateBodySync(entity, proc); // Comentado temporalmente por variables faltantes en la entidad

        // 2. Lógica de Cabeza y Brazos (IK)
        // updateHeadLook(entity, proc); // Comentado temporalmente por variables faltantes en la entidad
        // updateArmGrabIK(entity, proc, partialTick); // Comentado temporalmente por dependencias faltantes
        // updateLegArmor(entity, proc); // Comentado temporalmente por AnimState incorrecto
    }

    // ── Helpers Estáticos (Invocados por GalathModel) ─────────────────────────

    public static void applyGalathBones(BaseNpcEntity entity, AnimationProcessor<?> proc, float partial) {
        // 🚨 CORREGIDO: Eliminamos ClientProxy.IS_PRELOADING
        // boolean hasSkirt = true; // Forzado a true por ahora para evitar error de NpcRenderUtil

        // setBoneVisible(proc, "skirt", hasSkirt);
        // setBoneVisible(proc, "cheekRBelowSkirt", !hasSkirt);
        // setBoneVisible(proc, "cheekLBelowSkirt", !hasSkirt);
    }

    private static void setBoneVisible(AnimationProcessor<?> proc, String name, boolean visible) {
        GeoBone bone = proc.getBone(name);
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
}