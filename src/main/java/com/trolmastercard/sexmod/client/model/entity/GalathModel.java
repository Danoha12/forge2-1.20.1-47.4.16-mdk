package com.trolmastercard.sexmod.client.model.entity;

import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.client.model.entity.MangleLieModel;
import com.trolmastercard.sexmod.client.model.BaseNpcModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.command.FutaCommand;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.NpcPositionUtil;
import com.trolmastercard.sexmod.util.RgbColor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.GeoBone; // ✅ CORRECTOimport software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animation.AnimationState;

/**
 * GalathModel — Portado a 1.20.1 / GeckoLib 4.
 * * Modelo ultra-complejo para GalathEntity.
 * * Maneja cinemática inversa (IK) para los ojos y brazos, físicas de ropa
 * y estados combinados con MangleLie.
 */
public class GalathModel extends BaseNpcModel<BaseNpcEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/entity/galath/galath.png");

    private float headSwayPrev = 0.0F;
    private long swordStart = -1L;
    private long swordEnd = -1L;

    @Override
    protected ResourceLocation[] getGeoFiles() {
        return new ResourceLocation[] {
                new ResourceLocation("sexmod", "geo/galath/galath.geo.json"),            // [0] Base
                new ResourceLocation("sexmod", "geo/galath/galath.geo.json"),            // [1] Armada
                new ResourceLocation("sexmod", "geo/galath/galath_con_mang.geo.json")    // [2] Con MangleLie
        };
    }

    @Override
    public ResourceLocation getModelResource(BaseNpcEntity entity) {
        ResourceLocation[] geos = getGeoFiles();

        // Si Galath tiene a su compañera MangleLie activa, usamos el modelo combinado
        if (entity instanceof GalathEntity galath && galath.getMangleLie(false) != null) {
            return geos[2];
        }

        int idx = entity.getEntityData().get(BaseNpcEntity.DATA_OUTFIT_INDEX);
        return (idx >= 0 && idx < geos.length) ? geos[idx] : geos[0];
    }

    @Override
    public ResourceLocation getTextureResource(BaseNpcEntity entity) { return TEXTURE; }

    @Override
    public ResourceLocation getAnimationResource(BaseNpcEntity entity) {
        return new ResourceLocation("sexmod", "animations/galath/galath.animation.json");
    }

    // ── Animaciones Custom (Lógica procedimental) ───────────────────────────

    @Override
    public void setCustomAnimations(BaseNpcEntity entity, long instanceId, AnimationState<BaseNpcEntity> animState) {
        // 1. IK de Masturbación (Molang & Variables)
        updateMasturbateIK(entity, animState.getPartialTick());

        super.setCustomAnimations(entity, instanceId, animState);

        // 2. Lógica de Huesos Dinámicos
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
        updateCoinHidden(entity);

        // 3. Pasar rotación de cabeza a la entidad para cálculos de sonido/partículas
        if (entity instanceof GalathEntity galath) {
            GeoBone head = getAnimationProcessor().getBone("head");
            if (head != null) {
                galath.headRotX = head.getRotX();
            }

            // Bridge con MangleLieModel si es necesario
            if (galath.isSexModeActive()) {
                MangleLieModel.applyGalathBones(galath, getAnimationProcessor(), animState.getPartialTick());
            }
        }
    }

    // ── Implementaciones Técnicas ───────────────────────────────────────────

    private void updateMasturbateIK(BaseNpcEntity entity, float partialTick) {
        if (entity.getAnimState() != AnimState.MASTERBATE) return;

        var mc = Minecraft.getInstance();
        var player = entity.getOwnerPlayer();
        if (player == null) player = mc.player;
        if (player == null) return;

        Vec3 offset = NpcPositionUtil.getOffsetToPlayer(entity, player, partialTick)
                .subtract(entity.getBoneOffset("head"));

        float yaw = (float) MathUtil.normalizeAngle(Math.atan2(offset.z, offset.x)) - (float) Math.toRadians(entity.getYRot());
        float pitch = (float) MathUtil.normalizeAngle(Math.atan2(offset.y, Math.sqrt(offset.x * offset.x + offset.z * offset.z)));

        // Inyectamos valores al MathParser de GeckoLib 4 para que las variables de la animación .json reaccionen
        var parser = software.bernie.geckolib.loading.math.MathParser.INSTANCE;
        double dist = Math.abs(offset.x) + Math.abs(offset.y) + Math.abs(offset.z);

        try {
            parser.setMemoizedValue("pitch", dist * 7.0 - 20.0 + Math.toDegrees(pitch) - 80.0);
            parser.setMemoizedValue("yaw", Math.toDegrees(yaw) + 90.0);
        } catch (Exception ignored) {}
    }

    private void updateBoostWings(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath)) return;
        float yawOffset = 0.0F;
        AnimState state = galath.getAnimState();

        if (state == AnimState.BOOST) {
            int tick = AnimState.BOOST.getTicksPlaying(1);
            if (tick > 13 && tick < 40) yawOffset = 45.0F;
        } else if (state != AnimState.KNOCK_OUT_FLY && state != AnimState.KNOCK_OUT_SLOW) {
            return;
        }

        GeoBone rotTool = getAnimationProcessor().getBone("rotationTool");
        if (rotTool == null) return;

        var yp = galath.getYawPitch();
        float partial = Minecraft.getInstance().getPartialTick();

        // Conversión a Radianes para GeckoLib 4
        float rotX = (float) Math.toRadians(MathUtil.lerp(yp.pitchPrev + yawOffset, yp.pitch + yawOffset, partial));
        float rotZ = (float) Math.toRadians(MathUtil.lerp(yp.yawPrev, yp.yaw, partial));

        rotTool.updateRotation(rotX, rotTool.getRotY(), rotZ);
    }

    private void updateSwordAttack(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath) || galath.getAnimState() != AnimState.ATTACK_SWORD) {
            swordStart = -1L; swordEnd = -1L; return;
        }

        int frame = galath.getAttackAnimIdx(); // Reemplazo de field az()
        if (frame == 24 && swordStart == -1L) {
            swordStart = entity.level().getGameTime();
            swordEnd = swordStart + 8L;
        }
        if (frame < 24 || frame > 32) return;

        GeoBone body = getAnimationProcessor().getBone("body");
        if (body == null) return;

        float partial = Minecraft.getInstance().getPartialTick();
        RgbColor offset = galath.getBodySwayAt(entity.level().getGameTime());
        float t = ((float) (entity.level().getGameTime() + partial) - swordStart) / (float) (swordEnd - swordStart);
        RgbColor lerped = MathUtil.lerp(offset, RgbColor.ZERO, t);

        // Aplicamos el balanceo (Sway) al cuerpo en radianes
        body.updateRotation(Math.toRadians(lerped.r()), body.getRotY(), body.getRotZ());
        body.updatePosition(body.getPosX(), body.getPosY() + lerped.g(), body.getPosZ() + lerped.b());
    }

    private void updateClothingBones(BaseNpcEntity entity) {
        AnimationProcessor<BaseNpcEntity> proc = getAnimationProcessor();
        GeoBone nipR = proc.getBone("nippleR");
        GeoBone braL = proc.getBone("braBoobL");
        GeoBone slip = proc.getBone("slip");

        boolean isNude = entity instanceof GalathEntity g && g.isNudeMode();
        boolean sexAction = AnimState.anyOf(entity.getAnimState(), AnimState.PUSSY_LICKING, AnimState.MASTERBATE_SITTING);

        if (nipR != null) nipR.setHidden(!isNude);
        if (braL != null) proc.getBone("braBoobL").setHidden(isNude);
        if (proc.getBone("braBoobR") != null) proc.getBone("braBoobR").setHidden(isNude);
        if (slip != null) slip.setHidden(!(isNude || sexAction));
    }

    private void updateFutaBones() {
        boolean enabled = FutaCommand.futaModeEnabled;
        setBoneHidden("futaCock", !enabled);
        setBoneHidden("futaBallLL", !enabled);
        setBoneHidden("futaBallLR", !enabled);
    }

    private void updateWings(BaseNpcEntity entity) {
        boolean hasWings = entity instanceof GalathEntity g && g.hasWings();
        setBoneHidden("wings", !hasWings);
    }

    private void updateBodySnapshot(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath)) return;
        GeoBone body = getAnimationProcessor().getBone("body");
        if (body == null) return;
        galath.bodyRotY = body.getRotY();
        galath.bodyScaleY = body.getScaleY();
    }

    private void updatePussyLickingIK(BaseNpcEntity entity) {
        if (entity.getAnimState() != AnimState.PUSSY_LICKING || !(entity instanceof GalathEntity galath)) return;

        GeoBone head = getAnimationProcessor().getBone("head");
        if (head == null) return;

        float tick = Minecraft.getInstance().getPartialTick() + entity.level().getGameTime();
        RgbColor sway = galath.getHeadSwayAt(tick);

        // Sumamos el balanceo de la cabeza (IK procedimental)
        head.updateRotation(head.getRotX() + (float)Math.toRadians(sway.r()),
                head.getRotY() + (float)Math.toRadians(sway.b()),
                head.getRotZ() + (float)Math.toRadians(sway.g()));
    }

    private void updateKnockOutFly(BaseNpcEntity entity) {
        if (!(entity instanceof GalathEntity galath) || galath.getAnimState() != AnimState.KNOCK_OUT_FLY) return;

        GeoBone body = getAnimationProcessor().getBone("body");
        if (body == null) return;

        Vec3 v = MangleLieModel.computeBodyVec(entity, Minecraft.getInstance().getPartialTick());
        body.updateRotation(-(float)v.x, body.getRotY(), body.getRotZ());
        body.updatePosition(body.getPosX(), (float)v.y, (float)v.z);
    }

    private void updateTransitionBones(BaseNpcEntity entity) {
        if (entity.getAnimState() == AnimState.HUG_MANG) {
            GeoBone body2 = getAnimationProcessor().getBone("body2");
            if (body2 != null) {
                body2.updatePosition(0.0F, -0.53F, -40.05F);
            }
        }
    }

    private void updateCoinHidden(BaseNpcEntity entity) {
        if (entity instanceof PlayerKoboldEntity) {
            setBoneHidden("coin", true);
        }
    }

    private void setBoneHidden(String name, boolean hidden) {
        GeoBone bone = getAnimationProcessor().getBone(name);
        if (bone != null) bone.setHidden(hidden);
    }

    @Override public String[] getHelmetBones() { return new String[]{ "armorHelmet" }; }
}