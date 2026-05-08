package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.client.model.entity.MangleLieModel;
import com.trolmastercard.sexmod.client.model.BaseNpcModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.command.FutaCommand;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.NpcPositionUtil;
import com.trolmastercard.sexmod.util.RgbaColor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * GalathModel — Portado a 1.20.1 / GeckoLib 4.
 * REPARADO: Casteos de CoreGeoBone a GeoBone y limpieza de tipos.
 */
public class GalathModel extends BaseNpcModel<GalathEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/entity/galath/galath.png");
    private long swordStart = -1L;
    private long swordEnd = -1L;

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/galath/galath.geo.json"),
                new ResourceLocation("sexmod", "geo/galath/galath.geo.json"),
                new ResourceLocation("sexmod", "geo/galath/galath_con_mang.geo.json")
        };
    }

    @Override
    public ResourceLocation getModelResource(GalathEntity entity) {
        ResourceLocation[] geos = getGeoFiles();
        if (entity.getMangleLie(false) != null) {
            return geos[2];
        }
        int idx = entity.getEntityData().get(BaseNpcEntity.DATA_OUTFIT_INDEX);
        return (idx >= 0 && idx < geos.length) ? geos[idx] : geos[0];
    }

    @Override
    public ResourceLocation getTextureResource(GalathEntity entity) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(GalathEntity entity) {
        return new ResourceLocation("sexmod", "animations/galath/galath.animation.json");
    }

    @Override
    public void setCustomAnimations(GalathEntity entity, long instanceId, AnimationState<GalathEntity> animState) {
        updateMasturbateIK(entity, animState.getPartialTick());

        super.setCustomAnimations(entity, instanceId, animState);

        updateBoostWings(entity);
        updateRapeCharge(entity);
        updateSwordAttack(entity);
        updateKnockOutFly(entity);
        updateClothingBones(entity);
        updateWings(entity);
        updateFutaBones();
        updateBodySnapshot(entity);
        updatePussyLickingIK(entity);
        updateTransitionBones(entity);

        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone head = (GeoBone) getAnimationProcessor().getBone("head");
        if (head != null) {
            entity.headRotX = head.getRotX();
        }

        if (entity.isSexModeActive()) {
            MangleLieModel.applyGalathBones(entity, getAnimationProcessor(), animState.getPartialTick());
        }
    }

    private void updateMasturbateIK(GalathEntity entity, float partialTick) {
        if (entity.getAnimState() != AnimState.MASTERBATE) return;

        var player = entity.getOwnerPlayer();
        if (player == null) player = Minecraft.getInstance().player;
        if (player == null) return;

        Vec3 offset = NpcPositionUtil.getOffsetToPlayer(entity, player, partialTick)
                .subtract(entity.getBoneOffset("head"));

        float yaw = (float) MathUtil.normalizeAngle((float) Math.atan2(offset.z, offset.x)) - (float) Math.toRadians(entity.getYRot());
        float pitch = (float) MathUtil.normalizeAngle((float) Math.atan2(offset.y, Math.sqrt(offset.x * offset.x + offset.z * offset.z)));

        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone head = (GeoBone) getAnimationProcessor().getBone("head");
        if (head != null) {
            double dist = Math.abs(offset.x) + Math.abs(offset.y) + Math.abs(offset.z);
            float customPitch = (float) Math.toRadians(dist * 7.0 - 20.0 + Math.toDegrees(pitch) - 80.0);
            float customYaw = (float) Math.toRadians(Math.toDegrees(yaw) + 90.0);

            head.setRotX(head.getRotX() + customPitch);
            head.setRotY(head.getRotY() + customYaw);
        }
    }

    private void updateBoostWings(GalathEntity entity) {
        float yawOffset = 0.0F;
        AnimState state = entity.getAnimState();

        if (state == AnimState.BOOST) {
            int tick = entity.getAnimTick();
            if (tick > 13 && tick < 40) yawOffset = 45.0F;
        } else if (state != AnimState.KNOCK_OUT_FLY && state != AnimState.FLY) {
            return;
        }

        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone rotTool = (GeoBone) getAnimationProcessor().getBone("rotationTool");
        if (rotTool == null) return;

        var yp = entity.getYawPitch();
        float partial = Minecraft.getInstance().getPartialTick();

        float rotX = (float) Math.toRadians(MathUtil.lerp(yp.pitchPrev + yawOffset, yp.pitch + yawOffset, partial));
        float rotZ = (float) Math.toRadians(MathUtil.lerp(yp.yawPrev, yp.yaw, partial));

        rotTool.updateRotation(rotX, rotTool.getRotY(), rotZ);
    }

    private void updateSwordAttack(GalathEntity entity) {
        if (entity.getAnimState() != AnimState.ATTACK_SWORD) {
            swordStart = -1L; swordEnd = -1L; return;
        }

        int frame = entity.getAttackAnimIdx();
        if (frame == 24 && swordStart == -1L) {
            swordStart = entity.level().getGameTime();
            swordEnd = swordStart + 8L;
        }
        if (frame < 24 || frame > 32) return;

        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone body = (GeoBone) getAnimationProcessor().getBone("body");
        if (body == null) return;

        float partial = Minecraft.getInstance().getPartialTick();
        RgbaColor offset = entity.getBodySwayAt(entity.level().getGameTime());
        float t = ((float) (entity.level().getGameTime() + partial) - swordStart) / (float) (swordEnd - swordStart);

        RgbaColor lerped = RgbaColor.lerp(offset, RgbaColor.ZERO, t);

        body.updateRotation((float)Math.toRadians(lerped.r()), body.getRotY(), body.getRotZ());
        body.updatePosition(body.getPosX(), body.getPosY() + (float)lerped.g(), body.getPosZ() + (float)lerped.b());
    }

    private void updateClothingBones(GalathEntity entity) {
        boolean isNude = entity.isNudeMode();
        boolean sexAction = AnimState.anyOf(entity.getAnimState(), AnimState.PUSSY_LICKING, AnimState.MASTERBATE_SITTING);

        setBoneHidden("nippleR", !isNude);
        setBoneHidden("braBoobL", isNude);
        setBoneHidden("braBoobR", isNude);
        setBoneHidden("slip", !(isNude || sexAction));
    }

    private void updateFutaBones() {
        boolean enabled = FutaCommand.futaModeEnabled;
        setBoneHidden("futaCock", !enabled);
        setBoneHidden("futaBallLL", !enabled);
        setBoneHidden("futaBallLR", !enabled);
    }

    private void updateWings(GalathEntity entity) {
        setBoneHidden("wings", !entity.hasWings());
    }

    private void updateBodySnapshot(GalathEntity entity) {
        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone body = (GeoBone) getAnimationProcessor().getBone("body");
        if (body == null) return;
        entity.bodyRotY = body.getRotY();
        entity.bodyScaleY = body.getScaleY();
    }

    private void updatePussyLickingIK(GalathEntity entity) {
        if (entity.getAnimState() != AnimState.PUSSY_LICKING) return;

        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone head = (GeoBone) getAnimationProcessor().getBone("head");
        if (head == null) return;

        float tick = Minecraft.getInstance().getPartialTick() + entity.level().getGameTime();
        RgbaColor sway = entity.getHeadSwayAt(tick);

        head.updateRotation(head.getRotX() + (float)Math.toRadians(sway.r()),
                head.getRotY() + (float)Math.toRadians(sway.b()),
                head.getRotZ() + (float)Math.toRadians(sway.g()));
    }

    private void updateKnockOutFly(GalathEntity entity) {
        if (entity.getAnimState() != AnimState.KNOCK_OUT_FLY) return;

        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone body = (GeoBone) getAnimationProcessor().getBone("body");
        if (body == null) return;

        Vec3 v = MangleLieModel.computeBodyVec(entity, Minecraft.getInstance().getPartialTick());
        body.updateRotation(-(float)v.x, body.getRotY(), body.getRotZ());
        body.updatePosition(body.getPosX(), (float)v.y, (float)v.z);
    }

    private void updateTransitionBones(GalathEntity entity) {
        if (entity.getAnimState() == AnimState.HUG_MANG) {
            // 🚨 REPARADO: Casteo a (GeoBone)
            GeoBone body2 = (GeoBone) getAnimationProcessor().getBone("body2");
            if (body2 != null) body2.updatePosition(0.0F, -0.53F, -40.05F);
        }
    }

    @Override
    protected void setBoneHidden(String name, boolean hidden) {
        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone bone = (GeoBone) getAnimationProcessor().getBone(name);
        if (bone != null) bone.setHidden(hidden);
    }

    @Override public String[] getHelmetBones() { return new String[]{ "armorHelmet" }; }

    private void updateRapeCharge(GalathEntity entity) {
        // 🚨 REPARADO: Casteo a (GeoBone)
        GeoBone chargeBone = (GeoBone) getAnimationProcessor().getBone("chargeEffect");
        if (chargeBone != null) {
            chargeBone.setHidden(entity.getAnimState() != AnimState.RAPE_CHARGE);
        }
    }
}