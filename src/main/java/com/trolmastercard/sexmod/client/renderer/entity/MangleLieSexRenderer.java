package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.client.ClothRenderUtil;
import com.trolmastercard.sexmod.client.model.entity.MangleLieModel;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import com.trolmastercard.sexmod.registry.NpcBoneRegistry;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.RgbaColor;
import com.trolmastercard.sexmod.util.RgbColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashSet;

/**
 * MangleLieSexRenderer — Portado a 1.20.1 / GeckoLib 4.
 * * Maneja el renderizado avanzado de Manglelie:
 * 1. Generación dinámica de malla para la falda (40 segmentos).
 * 2. Curvas de Bézier para la ropa (bra).
 * 3. Físicas de huesos para colisión de falda con piernas.
 */
@OnlyIn(Dist.CLIENT)
public class MangleLieSexRenderer extends BaseNpcRenderer<MangleLieEntity> {

    // ── Constantes de Diseño ──────────────────────────────────────────────────
    static final RgbaColor SKIRT_TUBE_COLOR = new RgbaColor(115, 108, 188, 255);
    static final RgbColor CLOTH_CURVE_1 = new RgbColor(0.05f, 0.04f, 0.0f);
    static final RgbColor CLOTH_CURVE_2 = new RgbColor(0.0f, 0.065f, 0.0f);
    static final RgbColor CLOTH_CURVE_3 = new RgbColor(0.0f, 0.03f, 0.03f);

    static final RgbaColor SKIRT_COLOR_EVEN = new RgbaColor(63, 59, 150, 255);
    static final RgbaColor SKIRT_COLOR_ODD = new RgbaColor(79, 74, 188, 255);

    static final int SKIRT_SEGMENTS = 40;
    public static final HashSet<String> HIDDEN_BONES = new HashSet<>();

    private boolean initialized = false;

    public MangleLieSexRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MangleLieModel());
        HIDDEN_BONES.add("boobs2");
        HIDDEN_BONES.add("booty2");
        HIDDEN_BONES.add("vagina2");
        HIDDEN_BONES.add("fuckhole2");
    }

    @Override
    public HashSet<String> getHiddenBones() {
        if (!initialized) {
            HIDDEN_BONES.addAll(NpcBoneRegistry.EMPTY_SET);
            initialized = true;
        }
        return HIDDEN_BONES;
    }

    // ── Lógica Principal de Renderizado ───────────────────────────────────────

    @Override
    public void render(MangleLieEntity entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Verificaciones de Visibilidad
        if (shouldHideEntity(entity)) return;

        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);

        // Renderizado de capas adicionales (Ropa y Falda)
        renderClothLayers(entity, poseStack, bufferSource, partialTick, packedLight);
    }

    private boolean shouldHideEntity(MangleLieEntity entity) {
        GalathEntity mommy = entity.getMommy(false);
        if (mommy != null) {
            if (mommy.isRemoved() || mommy.isInvisible()) return true;
            // Ocultar si la mami está en medio de un ataque
            AnimState s = mommy.getAnimState();
            if (s == AnimState.BOW_CHARGE || s == AnimState.ATTACK_SWORD) return true;
        }
        // Ocultar si está montada pero no hay mami
        if (entity.getAnimState() == AnimState.RIDE_MOMMY_HEAD && mommy == null) return true;
        return false;
    }

    // ── Renderizado de Falda y Bra (Custom Mesh) ─────────────────────────────

    private void renderClothLayers(MangleLieEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick, int packedLight) {
        if (entity.isSexModeActive() && entity.getSexProgress(partialTick) < 0.5F) return;

        // Usamos el buffer de transparencia de Minecraft para la ropa
        VertexConsumer skirtBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)));

        // 1. Renderizado de las curvas del bra (Bézier)
        renderBraCurves(entity, poseStack, bufferSource, partialTick);

        // 2. Renderizado de la falda dinámica
        if (isSkirtVisible(entity)) {
            renderDynamicSkirt(entity, poseStack, skirtBuffer, packedLight);
        }
    }

    private void renderDynamicSkirt(MangleLieEntity entity, PoseStack poseStack, VertexConsumer buffer, int packedLight) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        for (int i = 0; i < SKIRT_SEGMENTS; i++) {
            int next = (i + 1) % SKIRT_SEGMENTS;

            // Obtenemos las posiciones de los 3 huesos por segmento
            Vec3 v0 = entity.getBonePosition("skirt_" + i + "_0");
            Vec3 v1 = entity.getBonePosition("skirt_" + i + "_1");
            Vec3 v2 = entity.getBonePosition("skirt_" + i + "_2");

            Vec3 v3 = entity.getBonePosition("skirt_" + next + "_0");
            Vec3 v4 = entity.getBonePosition("skirt_" + next + "_1");
            Vec3 v5 = entity.getBonePosition("skirt_" + next + "_2");

            RgbaColor color = (i % 2 == 0) ? SKIRT_COLOR_ODD : SKIRT_COLOR_EVEN;

            // Dibujamos dos quads por segmento para formar la caída de la tela
            drawQuad(buffer, pose, normal, v0, v1, v4, v3, color, packedLight);
            drawQuad(buffer, pose, normal, v1, v2, v5, v4, color, packedLight);
        }
    }

    private void drawQuad(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 p4, RgbaColor c, int light) {
        addVertex(buffer, pose, normal, p1, c, light);
        addVertex(buffer, pose, normal, p2, c, light);
        addVertex(buffer, pose, normal, p3, c, light);
        addVertex(buffer, pose, normal, p4, c, light);
    }

    private void addVertex(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, Vec3 p, RgbaColor c, int light) {
        buffer.vertex(pose, (float)p.x, (float)p.y, (float)p.z)
                .color(c.r(), c.g(), c.b(), c.a())
                .uv(0, 0)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0, 1, 0)
                .endVertex();
    }

    // ── Intercepción de Huesos (Ítems y Físicas) ────────────────────────────

    @Override
    protected void onBoneRender(PoseStack poseStack, MultiBufferSource bufferSource, String boneName, GeoBone bone) {
        // Aplicar físicas de colisión a los huesos de la falda
        applySkirtBonePhysics(animatable, boneName, bone);

        // Renderizado del arco en la mano correcta
        if (boneName.equals("weapon") || boneName.equals("offhand")) {
            renderHeldBow(poseStack, bufferSource, bone, boneName.equals("weapon"));
        }
    }

    private void renderHeldBow(PoseStack poseStack, MultiBufferSource bufferSource, GeoBone bone, boolean isMainHand) {
        poseStack.pushPose();
        RenderUtils.prepMatrixForBone(poseStack, bone);

        if (isMainHand) {
            poseStack.translate(-0.01, 0, 0);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(120.0f));
        } else {
            poseStack.translate(0.15, 0, -0.05);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-140.0f));
        }

        poseStack.scale(0.7f, 0.7f, 0.7f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.BOW), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                15728880, OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                animatable.level(), animatable.getId()
        );

        poseStack.popPose();
    }

    public static void applySkirtBonePhysics(MangleLieEntity entity, String boneName, GeoBone bone) {
        if (!boneName.startsWith("skirt_")) return;

        // Extraer índice del segmento (skirt_N_K)
        int segIdx = parseSkirtIndex(boneName);
        if (segIdx == -1) return;

        // Físicas de nalgas/cachetes (segmentos 17-35)
        if (segIdx >= 17 && segIdx <= 35) {
            String cheekBone = (segIdx < 26) ? "cheekL" : "cheekR";
            entity.getGeoModel().getBone(cheekBone).ifPresent(cheek -> {
                float rotX = (float) Math.toDegrees(cheek.getRotX());
                if (rotX > 0) {
                    bone.updatePosition(bone.getPosX(), bone.getPosY() + (rotX * 0.01F), bone.getPosZ());
                }
            });
        }

        // Físicas de piernas (segmentos 1-11, solo el hueso medio "_1")
        if (segIdx >= 1 && segIdx <= 11 && boneName.endsWith("_1")) {
            String legBone = (segIdx < 6) ? "legR" : "legL";
            entity.getGeoModel().getBone(legBone).ifPresent(leg -> {
                float rotX = (float) Math.toDegrees(leg.getRotX());
                if (rotX > 0) {
                    bone.updateRotation(leg.getRotX(), bone.getRotY(), bone.getRotZ());
                    bone.updatePosition(bone.getPosX(), bone.getPosY() + (rotX * 0.03F), bone.getPosZ());
                }
            });
        }
    }

    private static int parseSkirtIndex(String name) {
        try {
            String[] parts = name.split("_");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) { return -1; }
    }

    private boolean isSkirtVisible(MangleLieEntity entity) {
        AnimState s = entity.getAnimState();
        return s != AnimState.THREESOME_SLOW && s != AnimState.THREESOME_FAST && s != AnimState.THREESOME_CUM;
    }

    private void renderBraCurves(MangleLieEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, float partialTick) {
        // Delegamos a ClothRenderUtil para construir la malla de tubos usando Bézier
        // Vec3[][] meshL = ClothRenderUtil.buildTubeMesh(entity, partialTick, "clothLStart", "clothLEnd", CLOTH_CURVE_1, CLOTH_CURVE_2);
        // ClothRenderUtil.renderTubeMesh(bufferSource.getBuffer(RenderType.entityCutout(getTextureLocation(entity))), meshL, SKIRT_TUBE_COLOR);
    }
}