package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.trolmastercard.sexmod.util.RgbaColor;
import com.trolmastercard.sexmod.util.VectorMathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * FishingLineSegmentRenderer — Portado a 1.20.1.
 * * Dibuja una cinta plana compuesta por Quads.
 * * Puede retorcerse y ondular usando modificadores senoidales en tiempo real.
 */
public class FishingLineSegmentRenderer {

    // ── Renderizado Principal ────────────────────────────────────────────────

    /**
     * Dibuja el segmento de hilo.
     * @param poseStack La matriz de la cámara actual (VITAL en 1.20.1)
     * @param builder   El BufferBuilder activo
     * @param mc        Instancia de Minecraft
     * @param config    Configuración de la cinta
     */
    public static void render(PoseStack poseStack, BufferBuilder builder, Minecraft mc, Config config) {
        Vec3[] quad = {
                new Vec3(-config.halfWidth, -config.halfHeight, 0.0),
                new Vec3(-config.halfWidth, config.halfHeight, 0.0),
                new Vec3(config.halfWidth, config.halfHeight, 0.0),
                new Vec3(config.halfWidth, -config.halfHeight, 0.0)
        };

        // Dirección inicial "hacia adelante" en -Z
        Vec3 forward = new Vec3(0.0, 0.0, -config.initialLength);
        Vec3 cursor = VectorMathUtil.rotateAroundY(forward.normalize(), config.initialAngle);

        // Construir el anillo de posiciones anterior
        Vec3[] prevRing = new Vec3[4];
        System.arraycopy(applyAngle(quad, cursor), 0, prevRing, 0, 4);

        java.util.List<Vec3[]> rings = new java.util.ArrayList<>();
        float time = mc.player != null ? mc.player.tickCount + mc.getFrameTime() : 0.0F;

        // Calcular todos los anillos de la cinta
        for (int seg = 0; seg <= config.segmentCount; seg++) {
            float t = 1.0F - (float) seg / config.segmentCount;
            Vec3[] ring = new Vec3[4];
            for (int v = 0; v < 4; v++) {
                Vec3 base = quad[v];
                ring[v] = new Vec3(base.x * t, base.y, base.z).add(cursor);
            }
            rings.add(ring);

            // Avanzar el cursor sumando el modificador dinámico de este segmento
            forward = VectorMathUtil.rotateAroundY(
                    VectorMathUtil.applyOffset(forward,
                            config.xModifier.compute(seg, time),
                            config.yModifier.compute(seg, time),
                            config.zModifier.compute(seg, time)),
                    config.initialAngle);
            cursor = cursor.add(forward);
        }

        // ── Emisión al Buffer (Estándar 1.20.1) ──

        // Extraemos la matriz actual de la cámara
        Matrix4f pose = poseStack.last().pose();

        // Configuramos el Shader correcto para color puro
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        emitQuads(builder, pose, prevRing, rings.get(0), config.color);
        for (int i = 0; i < config.segmentCount - 1; i++) {
            emitQuads(builder, pose, rings.get(i), rings.get(i + 1), config.color);
        }

        // En 1.20.1, BufferUploader es el que envía la malla terminada a la tarjeta gráfica
        BufferUploader.drawWithShader(builder.end());
    }

    // ── Helper Senoidal ──────────────────────────────────────────────────────

    public static float sineWave(float speed, float freq, float phase, int segIndex, float amplitude) {
        return (float) (Math.sin(speed * freq + phase * segIndex) * amplitude);
    }

    // ── Emisión de Quads ─────────────────────────────────────────────────────

    private static void emitQuads(BufferBuilder builder, Matrix4f pose, Vec3[] a, Vec3[] b, RgbaColor color) {
        // Cara Frontal
        putVertex(builder, pose, a[1], color); putVertex(builder, pose, a[2], color);
        putVertex(builder, pose, b[2], color); putVertex(builder, pose, b[1], color);
        // Cara Trasera
        putVertex(builder, pose, a[0], color); putVertex(builder, pose, a[1], color);
        putVertex(builder, pose, b[1], color); putVertex(builder, pose, b[0], color);
        // Cara Derecha
        putVertex(builder, pose, a[2], color); putVertex(builder, pose, a[3], color);
        putVertex(builder, pose, b[3], color); putVertex(builder, pose, b[2], color);
        // Cara Izquierda
        putVertex(builder, pose, a[0], color); putVertex(builder, pose, a[3], color);
        putVertex(builder, pose, b[3], color); putVertex(builder, pose, b[0], color);
    }

    private static void putVertex(BufferBuilder builder, Matrix4f pose, Vec3 v, RgbaColor color) {
        // En 1.20.1 es vital multiplicar la coordenada por la matriz (pose)
        builder.vertex(pose, (float) v.x, (float) v.y, (float) v.z)
                .color(color.r(), color.g(), color.b(), color.a())
                .endVertex();
    }

    private static Vec3[] applyAngle(Vec3[] quad, Vec3 dir) {
        Vec3[] out = new Vec3[4];
        for (int i = 0; i < 4; i++) out[i] = quad[i].add(dir);
        return out;
    }

    // ── Clases Internas ──────────────────────────────────────────────────────

    public static class Config {
        public final RgbaColor color;
        public final float initialAngle;
        public final int segmentCount;
        public final float initialLength;
        public final Modifier xModifier;
        public final Modifier yModifier;
        public final Modifier zModifier;
        public final float halfWidth;
        public final float halfHeight;

        public Config(RgbaColor color, float initialAngle, int segmentCount,
                      float initialLength, Modifier xMod, Modifier yMod, Modifier zMod,
                      float halfWidth, float halfHeight) {
            this.color = color;
            this.initialAngle = initialAngle;
            this.segmentCount = segmentCount;
            this.initialLength = initialLength;
            this.xModifier = xMod;
            this.yModifier = yMod;
            this.zModifier = zMod;
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
        }
    }

    @FunctionalInterface
    public interface Modifier {
        float compute(int index, float time);
    }
}