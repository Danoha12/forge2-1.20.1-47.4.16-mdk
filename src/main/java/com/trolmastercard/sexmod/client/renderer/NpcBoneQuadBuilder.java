package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.util.VectorRotateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * NpcBoneQuadBuilder — Portado a 1.20.1.
 * * Construye mallas de colisión/interacción (Quads) en espacio de mundo
 * * vinculadas a los huesos de un modelo animado.
 */
@OnlyIn(Dist.CLIENT)
public final class NpcBoneQuadBuilder {

    private NpcBoneQuadBuilder() {}

    // ── API Pública ──────────────────────────────────────────────────────────

    /** Construye una cápsula de 10 caras uniendo 3 huesos (ej: Escenas de BJ/Anal) */
    public static Vec3[][] buildCapsuleQuads(BaseNpcEntity npc, float partialTick, String b1, String b2, String b3, float hw1, float hd1, float hw2, float hd2, String ref) {
        Vec3[] corners = computeCapsuleCorners(npc, partialTick, b1, b2, b3, hw1, hd1, hw2, hd2, ref);
        return buildCapsuleFaces(corners);
    }

    /** Construye una caja de 6 caras uniendo 2 huesos (ej: Mating Press) */
    public static Vec3[][] buildBoxQuads(BaseNpcEntity npc, float partialTick, String b1, String b2, RgbColor ext1, RgbColor ext2) {
        Vec3[] corners = computeBoxCorners(npc, partialTick, b1, b2, ext1, ext2);
        return buildBoxFaces(corners);
    }

    // ── Cálculo de Vértices ──────────────────────────────────────────────────

    static Vec3[] computeCapsuleCorners(BaseNpcEntity npc, float partialTick, String b1, String b2, String b3, float hw1, float hd1, float hw2, float hd2, String ref) {
        // En GeckoLib 4, obtenemos el hueso directamente del procesador
        GeoBone refBone = npc.getAnimModel().getAnimationProcessor().getBone(ref);
        if (refBone == null) return new Vec3[12];

        // Convertimos la rotación del hueso (radianes) a grados si es necesario
        float rotY = refBone.getRotY();
        float rotZ = refBone.getRotZ();

        Vec3 pos1 = npc.getBonePosition(b1);
        Vec3 pos2 = npc.getBonePosition(b2);
        Vec3 pos3 = npc.getBonePosition(b3);

        Vec3[] c = new Vec3[12];
        // Definición de anillos (Anillo 1, Anillo de unión, Anillo 2)
        c[0] = new Vec3(hw1, 0, -hd1); c[1] = new Vec3(-hw1, 0, -hd1);
        c[2] = new Vec3(-hw1, 0, hd1); c[3] = new Vec3(hw1, 0, hd1);
        c[4] = new Vec3(hw1, hd1, 0);  c[5] = new Vec3(-hw1, hd1, 0);
        c[6] = new Vec3(-hw1, -hd1, 0); c[7] = new Vec3(hw1, -hd1, 0);
        c[8] = new Vec3(hw2, 0, -hd2); c[9] = new Vec3(-hw2, 0, -hd2);
        c[10] = new Vec3(-hw2, 0, hd2); c[11] = new Vec3(hw2, 0, hd2);

        for (int i = 0; i < 12; i++) {
            // Rotación por el Yaw de la entidad
            c[i] = VectorRotateUtil.rotateY(c[i], npc.getViewYRot(partialTick));
            // Rotación local del hueso (Solo a los primeros 4 para el "cap")
            if (i < 4) c[i] = VectorRotateUtil.rotateEuler(c[i], 0, rotY, rotZ);

            // Traslación a su respectivo hueso
            if (i < 4) c[i] = c[i].add(pos1);
            else if (i < 8) c[i] = c[i].add(pos2);
            else c[i] = c[i].add(pos3);
        }
        return c;
    }

    // ── Renderizado Moderno ──────────────────────────────────────────────────

    /**
     * Dibuja los Quads usando VertexConsumer (Estándar 1.20.1).
     * @param consumer El buffer donde escribir (ej: de MultiBufferSource)
     * @param poseStack La matriz de transformación actual
     */
    public static void renderQuads(VertexConsumer consumer, PoseStack poseStack, Vec3[][] quads, RgbaColor color) {
        PoseStack.Pose lastPose = poseStack.last();
        for (Vec3[] face : quads) {
            for (Vec3 v : face) {
                // En 1.20.1 usamos la matriz del PoseStack para transformar los vértices
                consumer.vertex(lastPose.pose(), (float)v.x, (float)v.y, (float)v.z)
                        .color(color.r, color.g, color.b, color.a)
                        .uv(0, 0)
                        .overlayCoordinate(0) // Overlay de daño (ninguno)
                        .uv2(240) // Brillo máximo (Lightmap)
                        .normal(lastPose.normal(), 0, 1, 0) // Normal hacia arriba por defecto
                        .endVertex();
            }
        }
    }

    // ── Posicionamiento ──────────────────────────────────────────────────────

    public static void applyNpcTranslation(Minecraft mc, BaseNpcEntity npc, float partialTick, PoseStack poseStack) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Evitar Z-Fighting
        poseStack.translate(0.0, 0.01, 0.0);

        // Interpolación moderna de posición
        double npcX = Mth.lerp(partialTick, npc.xo, npc.getX());
        double npcY = Mth.lerp(partialTick, npc.yo, npc.getY());
        double npcZ = Mth.lerp(partialTick, npc.zo, npc.getZ());

        double playerX = Mth.lerp(partialTick, player.xo, player.getX());
        double playerY = Mth.lerp(partialTick, player.yo, player.getY());
        double playerZ = Mth.lerp(partialTick, player.zo, player.getZ());

        // El renderizado siempre es relativo a la cámara (jugador)
        poseStack.translate(npcX - playerX, npcX - playerY, npcZ - playerZ);
    }

    // ── Ensamblado de Caras (Lógica original intacta) ────────────────────────

    static Vec3[][] buildCapsuleFaces(Vec3[] c) {
        return new Vec3[][]{
                {c[0], c[1], c[5], c[4]}, {c[1], c[2], c[6], c[5]},
                {c[3], c[2], c[6], c[7]}, {c[0], c[4], c[7], c[3]},
                {c[0], c[1], c[2], c[3]}, {c[4], c[5], c[9], c[8]},
                {c[9], c[10], c[6], c[5]}, {c[10], c[11], c[7], c[6]},
                {c[4], c[7], c[11], c[8]}, {c[8], c[9], c[10], c[11]}
        };
    }

    static Vec3[][] buildBoxFaces(Vec3[] c) {
        return new Vec3[][]{
                {c[0], c[1], c[2], c[3]}, {c[4], c[5], c[6], c[7]},
                {c[1], c[2], c[6], c[5]}, {c[3], c[7], c[4], c[0]},
                {c[1], c[0], c[4], c[5]}, {c[2], c[3], c[7], c[6]}
        };
    }

    public record RgbaColor(float r, float g, float b, float a) {
        public static final RgbaColor RED = new RgbaColor(1, 0, 0, 0.5f);
        public static final RgbaColor GREEN = new RgbaColor(0, 1, 0, 0.5f);
    }
}