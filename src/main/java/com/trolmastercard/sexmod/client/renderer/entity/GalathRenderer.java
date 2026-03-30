package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.FakeWorld;
import com.trolmastercard.sexmod.client.model.entity.GalathModel;
import com.trolmastercard.sexmod.client.particle.PhysicsParticleRenderer;
import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.RgbaColor;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashSet;

/**
 * GalathRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja efectos visuales avanzados: alas, físicas de pelo, lengua dinámica e IK.
 * * Integra interpolación de posición durante el vuelo y escenas de interacción.
 */
public class GalathRenderer extends BaseNpcRenderer<GalathEntity> {

    public static final HashSet<String> HIDDEN_BONES = new HashSet<>();
    static final RgbaColor TONGUE_COLOR = new RgbaColor(152, 45, 62, 255);
    static final RgbaColor TONGUE_SETTLED = new RgbaColor(84, 66, 88, 255);

    private boolean isInitialized = false;

    public GalathRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GalathModel());
    }

    // ── Gestión de Huesos Ocultos ───────────────────────────────────────────

    @Override
    public HashSet<String> getHiddenBones() {
        if (!isInitialized) {
            HIDDEN_BONES.add("static"); HIDDEN_BONES.add("turnable"); HIDDEN_BONES.add("slip");
            HIDDEN_BONES.add("boobs"); HIDDEN_BONES.add("booty"); HIDDEN_BONES.add("vagina");
            HIDDEN_BONES.add("fuckhole"); HIDDEN_BONES.add("futaBallLR"); HIDDEN_BONES.add("futaBallLL");
            HIDDEN_BONES.add("coin"); HIDDEN_BONES.add("pentagram");
            isInitialized = true;
        }
        return HIDDEN_BONES;
    }

    // ── Lógica Pre-Render (Interpolación y Alas) ────────────────────────────

    @Override
    public void preRender(PoseStack poseStack, GalathEntity entity, BakedGeoModel bakedModel,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {

        if (entity.level() instanceof FakeWorld || entity.isSummonedAway()) return;

        // 1. Interpolación de posición de cuerpo (Vuelo/Aproximación)
        Vec3 bodyOffset = computeBodyOffset(entity, partialTick);
        if (bodyOffset != null) entity.setBodyOffset(bodyOffset);

        // 2. Sincronización de ángulos
        entity.yBodyRot = entity.yRotO;
        GalathEntity.syncPreRenderAngles(entity, partialTick);

        // 3. Aplicar Yaw para estados especiales
        if (entity.getAnimState() == AnimState.MASTERBATE) {
            float yaw = entity.getYRot();
            entity.yBodyRot = entity.yHeadRot = entity.yHeadRotO = entity.yBodyRotO = entity.yRotO = yaw;
        }

        // 4. Renderizado de Alas (Tessellation personalizada)
        if (entity.hasWings()) {
            PhysicsParticleRenderer.renderGalathWings(entity, bufferSource, poseStack, partialTick);
        }

        super.preRender(poseStack, entity, bakedModel, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    // ── Intercepción de Huesos (IK y Físicas) ────────────────────────────────

    @Override
    protected void onBoneRender(PoseStack poseStack, GeoBone bone) {
        String name = bone.getName();
        Minecraft mc = Minecraft.getInstance();

        switch (name) {
            // Pelo: Reacciona a la inclinación de la cabeza
            case "hairBack" -> applyHairBackTilt(bone);
            case "hairDownSideL", "hairDownSideR" -> applyHairSideTilt(bone);

            // Cabeza: IK de mirada y balanceo en escenas
            case "head" -> {
                applyHeadTrackingIK(bone);
            }
            case "head3" -> applyMorningBlowjobHeadSway(bone);

            // Ítems y Lengua
            case "weapon" -> {
                if (animatable.hasSword()) renderSwordAtBone(poseStack, bone);
            }
            case "tongue", "mangTongue" -> renderTongueChain(poseStack, bone, name.equals("mangTongue"));

            // Ojos: Movimiento de iris dinámico
            case "irisL", "irisR", "irsisFaceR2", "irsisFaceR3" -> applyIrisOffset(bone);

            // Brazos: IK hacia el objetivo durante el RAPE_CHARGE
            case "armL", "armR" -> applyRapeChargeArmIK(bone);
        }
    }

    // ── Implementación de IK y Transformaciones ─────────────────────────────

    private void applyHairBackTilt(GeoBone bone) {
        GeoBone head = getAnimatable().getAnimatableInstanceCache().getManagerForId(animatable.getId()).getModelData().getBone("head");
        if (head == null) return;

        float headRotX = head.getRotX(); // En Radianes (GeckoLib 4)
        if (headRotX < 0) {
            bone.updateRotation(-headRotX, bone.getRotY(), bone.getRotZ());
        } else {
            float t = Math.min(1.0F, (float) Math.toDegrees(headRotX) / 45.0F);
            bone.updateRotation(-headRotX, bone.getRotY(), bone.getRotZ());
            bone.updatePosition(bone.getPosX(), bone.getPosY() + (t * 1.5F), bone.getPosZ());
        }
    }

    private void applyMorningBlowjobHeadSway(GeoBone bone) {
        if (!AnimState.isInState(animatable, AnimState.MORNING_BLOWJOB_SLOW, AnimState.MORNING_BLOWJOB_FAST)) return;

        float t = Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getFrameTime();
        float swayY = (float) (Math.sin(t * 0.1) * 0.1) + 0.2F;
        float swayZ = (float) Math.sin(t * 0.1) * 0.1F;

        bone.updateRotation(bone.getRotX(), bone.getRotY() + swayY, bone.getRotZ() + swayZ);
    }

    private void applyIrisOffset(GeoBone bone) {
        if (!AnimState.isInState(animatable, AnimState.MORNING_BLOWJOB_SLOW)) return;
        float t = Minecraft.getInstance().level.getGameTime() + Minecraft.getInstance().getFrameTime();
        float offset = (float) (Math.sin(t * 0.1) * -0.1);
        bone.updatePosition(bone.getPosX() + offset, bone.getPosY(), bone.getPosZ());
    }

    private void applyRapeChargeArmIK(GeoBone bone) {
        if (animatable.getAnimState() != AnimState.RAPE_CHARGE) return;
        var target = animatable.getVehicleTarget();
        if (target == null) return;

        Vec3 delta = target.position().subtract(animatable.position());
        Vec3 rotated = VectorMathUtil.rotateByYaw(delta, -animatable.getYRot());
        float armZ = (float) Math.toRadians(MathUtil.clamp(rotated.x * 45.0, -45.0, 45.0));

        bone.updateRotation(bone.getRotX(), bone.getRotY(), bone.getRotZ() + armZ);
    }

    // ── Renderizado de Ítems (Espada) ───────────────────────────────────────

    private void renderSwordAtBone(PoseStack poseStack, GeoBone bone) {
        ItemStack sword = animatable.getMainHandItem();
        if (sword.isEmpty()) return;

        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);

        // Ajuste de posición para que la empuñadura encaje en la mano
        poseStack.translate(0.0, -0.2, 0.0);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                sword, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                15728880, 655360, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(),
                animatable.level(), animatable.getId()
        );
        poseStack.popPose();
    }

    // ── Helper de Interpolación de Vuelo ────────────────────────────────────

    private Vec3 computeBodyOffset(GalathEntity entity, float partialTick) {
        float phase = entity.getApproachPhase();
        if (phase == -1.0F) return null;

        var target = entity.getVehicleTarget();
        if (target == null) return null;

        Vec3 targetPos = MathUtil.lerp(new Vec3(target.xo, target.yo, target.zo), target.position(), partialTick);

        if (phase >= 24 && phase <= 32) {
            Vec3 flyOffset = VectorMathUtil.rotate(new Vec3(0, 0, 3), entity.getYRot() + 180.0F);
            Vec3 dest = targetPos.add(0, target.getEyeHeight(), 0).add(flyOffset);
            return MathUtil.lerp(entity.getBodyOffset(), dest, 0.1F); // Suavizado simple
        }

        if (phase > 32 && phase <= 54) {
            Vec3 hoverOffset = VectorMathUtil.rotate(new Vec3(0, 0, 1.5), entity.getYRot() + 180.0F);
            return targetPos.add(hoverOffset);
        }
        return null;
    }

    // ── Placeholder de Lengua (Requiere tu sistema de partículas) ───────────

    private void renderTongueChain(PoseStack poseStack, GeoBone bone, boolean isMang) {
        // Aquí llamarías a PhysicsParticleRenderer.renderChain(...)
        // usando las constantes TONGUE_CONFIG_G o TONGUE_CONFIG_T que definiste.
    }
}