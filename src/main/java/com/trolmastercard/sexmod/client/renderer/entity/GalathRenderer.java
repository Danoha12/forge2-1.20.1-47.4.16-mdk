package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.model.entity.GalathModel;
import com.trolmastercard.sexmod.client.particle.PhysicsParticleRenderer;
import com.trolmastercard.sexmod.client.renderer.BaseNpcRenderer;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

public class GalathRenderer extends BaseNpcRenderer<GalathEntity> implements IBoneFilter {

    static final RgbaColor TONGUE_SETTLED = new RgbaColor(84, 66, 88, 255);

    public GalathRenderer(EntityRendererProvider.Context ctx) {
        // 🚨 REPARADO: Pasamos el modelo correctamente
        super(ctx, new GalathModel());
    }

    @Override
    public void preRender(PoseStack poseStack, GalathEntity entity, BakedGeoModel bakedModel,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {

        if (entity.level() == null || entity.isSummonedAway()) return;

        // 1. Interpolación de posición (Vuelo)
        Vec3 bodyOffset = computeBodyOffset(entity, partialTick);
        if (bodyOffset != null) entity.setBodyOffset(bodyOffset);

        // 2. Sincronización de ángulos
        entity.yBodyRot = entity.yRotO;
        GalathEntity.syncPreRenderAngles(entity, partialTick);

        // 3. Lógica especial de Yaw
        if (entity.getAnimState() == AnimState.MASTERBATE || entity.isKnockOutFlyActive()) {
            entity.yBodyRot = entity.yHeadRot = entity.yHeadRotO = entity.yBodyRotO = entity.yRotO = entity.getYRot();
        }

        // 4. Renderizado de Alas
        if (entity.hasWings()) {
            PhysicsParticleRenderer.renderGalathWings(entity, bufferSource, poseStack, partialTick);
        }

        super.preRender(poseStack, entity, bakedModel, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        // 5. Efectos post-render
        renderHairAndEffects(entity, poseStack, bufferSource, partialTick);
    }

    @Override
    public void renderRecursively(PoseStack poseStack, GalathEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String name = bone.getName();

        if (!isBoneVisible(animatable, name)) return;

        switch (name) {
            case "hairBack" -> applyHairBackTilt(bone);
            case "hairDownSideL", "hairDownSideR" -> applyHairSideTilt(bone);
            case "head" -> applyFlyAndSwordHeadIK(animatable, bone); // Pasamos animatable
            case "weapon" -> { if (animatable.hasSword()) renderSwordAtBone(animatable, poseStack, bone, bufferSource, packedLight); }
            case "irisL", "irisR" -> applyIrisOffset(animatable, bone);
            case "armL", "armR" -> applyRapeChargeArmIK(animatable, bone);
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private void applyHairBackTilt(GeoBone bone) {
        // 🚨 REPARADO: Usamos getGeoModel() en lugar de currentModel
        getGeoModel().getBone("head").ifPresent(head -> {
            bone.setRotX(bone.getRotX() - head.getRotX());
        });
    }

    private void renderSwordAtBone(GalathEntity entity, PoseStack poseStack, GeoBone bone, MultiBufferSource bufferSource, int packedLight) {
        ItemStack sword = entity.getMainHandItem();
        if (sword.isEmpty()) return;

        poseStack.pushPose();
        BoneMatrixUtil.applyBoneMatrix(poseStack, bone);
        poseStack.translate(0.0, -0.2, 0.0);
        Minecraft.getInstance().getItemRenderer().renderStatic(sword, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, 655360, poseStack, bufferSource, entity.level(), entity.getId());
        poseStack.popPose();
    }

    // 🚨 REPARADO: Ajuste de firma para IBoneFilter
    @Override
    public boolean isBoneVisible(BaseNpcEntity entity, String boneName) {
        return !NpcColorData.getHiddenBones().contains(boneName);
    }

    private Vec3 computeBodyOffset(GalathEntity entity, float partialTick) {
        float phase = entity.getApproachPhase();
        if (phase == -1f) return null;

        var target = entity.getVehicleTarget();
        if (target == null) return null;

        Vec3 targetPos = MathUtil.lerp(new Vec3(target.xo, target.yo, target.zo), target.position(), partialTick);

        if (MathUtil.inRange(phase, 24, 32)) {
            Vec3 flyOffset = VectorMathUtil.rotate(new Vec3(0, 0, 3), entity.getYRot() + 180f);
            Vec3 dest = targetPos.add(0, target.getEyeHeight(), 0).add(flyOffset);
            return MathUtil.lerp(entity.getBodyOffset(), dest, 0.1f);
        }

        if (MathUtil.inRange(phase, 32, 54)) {
            Vec3 hoverOffset = VectorMathUtil.rotate(new Vec3(0, 0, 1.5), entity.getYRot() + 180f);
            return targetPos.add(hoverOffset);
        }
        return null;
    }

    private void applyHairSideTilt(GeoBone bone) {
        getGeoModel().getBone("head").ifPresent(head -> {
            float headRotX = head.getRotX();
            if (headRotX < 0f) {
                bone.setRotX(bone.getRotX() - (headRotX / 2f));
            } else {
                float t = Math.min(1f, (float)Math.toDegrees(headRotX) / 45f);
                bone.setRotX(bone.getRotX() - headRotX);
                bone.setPosY(bone.getPosY() + t);
            }
        });
    }

    private void applyFlyAndSwordHeadIK(GalathEntity animatable, GeoBone bone) {
        var target = animatable.getVehicleTarget();
        if (target == null) return;
        Vec3 from = animatable.position();
        Vec3 to = target.position();
        Vec3 delta = from.subtract(to);
        Vec3 relDelta = VectorMathUtil.rotateByYaw(delta, animatable.getYRot());
        bone.setRotX(bone.getRotX() + (float) Math.atan2(delta.y, relDelta.z));
    }

    private void applyRapeChargeArmIK(GalathEntity animatable, GeoBone bone) {
        if (animatable.getAnimState() != AnimState.RAPE_CHARGE) return;
        var target = animatable.getVehicleTarget();
        if (target == null) return;
        Vec3 delta = target.position().subtract(animatable.position());
        Vec3 rotated = VectorMathUtil.rotateByYaw(delta, animatable.getYRot());
        bone.setRotZ(bone.getRotZ() + (float)Math.toRadians(Mth.clamp(rotated.x * 45.0, -45.0, 45.0)));
    }

    private void applyIrisOffset(GalathEntity animatable, GeoBone bone) {
        if (animatable.getAnimState() != AnimState.MORNING_BLOWJOB_SLOW) return;
        float t = Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getFrameTime();
        bone.setPosX(bone.getPosX() + (float)(Math.sin(t * 0.1) * -0.1));
    }

    private void renderHairAndEffects(GalathEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, float pt) {
        PhysicsParticleRenderer.renderHairStrands(entity, bufferSource, poseStack, pt, "hairStrandStartR", "hairStrandMidR", "hairStrandEndR", TONGUE_SETTLED);
        PhysicsParticleRenderer.renderHairStrands(entity, bufferSource, poseStack, pt, "hairStrandStartL", "hairStrandMidL", "hairStrandEndL", TONGUE_SETTLED);
        if (entity.hasPlayerBound()) PhysicsParticleRenderer.renderGalathStarRing(entity, bufferSource, poseStack, pt);
    }
}