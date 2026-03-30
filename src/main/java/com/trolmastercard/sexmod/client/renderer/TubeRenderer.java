package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete de renderizadores

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.util.VectorMathUtil; // Asumiendo tu clase utilitaria
import com.trolmastercard.sexmod.util.RgbaColor;      // Asumiendo tu clase de color
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;

/**
 * TubeRenderer — Portado a 1.20.1.
 * * Renderiza un tubo segmentado a lo largo de una curva paramétrica.
 * * Usado para cuerdas, tentáculos y efectos de conexión flexible.
 */
public class TubeRenderer {

    // ── Punto de Entrada Público ─────────────────────────────────────────────

    /**
     * Renderiza un tubo en el {@code consumer} según la {@code config}.
     *
     * @param poseStack Matriz de transformación actual (¡Vital en 1.20.1!)
     * @param consumer  Destino de los vértices (ej. MultiBufferSource)
     * @param mc        Instancia de Minecraft (para el tiempo de animación)
     * @param config    Parámetros del tubo
     */
    public static void render(PoseStack poseStack, VertexConsumer consumer, Minecraft mc, TubeConfig config) {
        // Obtenemos la matriz de transformación para aplicarla a los vértices
        Matrix4f pose = poseStack.last().pose();

        // Esquinas iniciales del corte transversal (rectángulo w × h en XY, apuntando a -Z)
        Vec3[] crossSection = {
                new Vec3(-config.halfWidth, -config.halfHeight, 0.0D),
                new Vec3(-config.halfWidth,  config.halfHeight, 0.0D),
                new Vec3( config.halfWidth,  config.halfHeight, 0.0D),
                new Vec3( config.halfWidth, -config.halfHeight, 0.0D)
        };

        // Dirección a la que apunta el tubo
        Vec3 dir = new Vec3(0.0D, 0.0D, -config.length);
        Vec3 dirNorm = VectorMathUtil.rotateEuler(dir.normalize(), config.startAngle);

        ArrayList<Vec3[]> frames = new ArrayList<>();

        // 1.20.1: mc.getFrameTime() ahora es mc.getPartialTick()
        float animTime = mc.player != null ? mc.player.tickCount + mc.getPartialTick() : 0.0F;

        // Construir el PRIMER frame (seg = 0)
        frames.add(buildFrame(crossSection, 0, config, dirNorm));

        // 🚨 Bucle corregido: empezamos en seg = 1 para no duplicar el frame inicial
        for (int seg = 1; seg <= config.segments; seg++) {
            // Perturbar dirección para este segmento
            dir = VectorMathUtil.perturbDirection(
                    dir,
                    config.yawOffset.sample(seg, animTime),
                    config.pitchOffset.sample(seg, animTime),
                    config.rollOffset.sample(seg, animTime)
            );
            dirNorm = dirNorm.add(dir);

            // Añadir nuevo frame
            frames.add(buildFrame(crossSection, seg, config, dirNorm));
        }

        // Renderizar la tira de quads usando el VertexConsumer
        for (int seg = 0; seg < frames.size() - 1; seg++) {
            writeQuad(pose, consumer, frames.get(seg), frames.get(seg + 1), config.color);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Vec3[] buildFrame(Vec3[] crossSection, int seg, TubeConfig cfg, Vec3 dirNorm) {
        float taper = 1.0F - ((float) seg / cfg.segments);
        Vec3[] frame = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            Vec3 c = crossSection[i];
            frame[i] = new Vec3(c.x * taper, c.y, c.z).add(dirNorm);
        }
        return frame;
    }

    /**
     * Escribe 4 quads (arriba, abajo, izquierda, derecha) conectando dos frames transversales.
     */
    static void writeQuad(Matrix4f pose, VertexConsumer buf, Vec3[] from, Vec3[] to, RgbaColor color) {
        int r = color.r, g = color.g, b = color.b, a = color.a;

        // Cara Superior
        vert(pose, buf, from[1], r, g, b, a);
        vert(pose, buf, from[2], r, g, b, a);
        vert(pose, buf,   to[2], r, g, b, a);
        vert(pose, buf,   to[1], r, g, b, a);

        // Cara Inferior
        vert(pose, buf, from[0], r, g, b, a);
        vert(pose, buf, from[1], r, g, b, a);
        vert(pose, buf,   to[1], r, g, b, a);
        vert(pose, buf,   to[0], r, g, b, a);

        // Cara Derecha
        vert(pose, buf, from[2], r, g, b, a);
        vert(pose, buf, from[3], r, g, b, a);
        vert(pose, buf,   to[3], r, g, b, a);
        vert(pose, buf,   to[2], r, g, b, a);

        // Cara Izquierda
        vert(pose, buf, from[0], r, g, b, a);
        vert(pose, buf, from[3], r, g, b, a);
        vert(pose, buf,   to[3], r, g, b, a);
        vert(pose, buf,   to[0], r, g, b, a);
    }

    /**
     * 🚨 1.20.1: Usamos la matriz 'pose' para transformar el vértice local al mundo real
     */
    private static void vert(Matrix4f pose, VertexConsumer buf, Vec3 v, int r, int g, int b, int a) {
        buf.vertex(pose, (float) v.x, (float) v.y, (float) v.z)
                .color(r, g, b, a)
                .endVertex();
    }

    // ── Helper de Onda Senoidal ──────────────────────────────────────────────

    public static float sinWave(float time, float freq, float phase, int seg, float amp) {
        return (float) (Math.sin(time * freq + phase * seg) * amp);
    }

    // ── Clases de Configuración ──────────────────────────────────────────────

    public static class TubeConfig {
        public RgbaColor color;
        public float startAngle;
        public int segments;
        public float length;
        public TubeAnimSampler yawOffset;
        public TubeAnimSampler pitchOffset;
        public TubeAnimSampler rollOffset;
        public float halfWidth;
        public float halfHeight;

        public TubeConfig(RgbaColor color, float startAngle, int segments, float length,
                          TubeAnimSampler yaw, TubeAnimSampler pitch, TubeAnimSampler roll,
                          float halfWidth, float halfHeight) {
            this.color = color;
            this.startAngle = startAngle;
            this.segments = segments;
            this.length = length;
            this.yawOffset = yaw;
            this.pitchOffset = pitch;
            this.rollOffset = roll;
            this.halfWidth = halfWidth;
            this.halfHeight = halfHeight;
        }

        public TubeConfig copy() {
            return new TubeConfig(color, startAngle, segments, length,
                    yawOffset, pitchOffset, rollOffset, halfWidth, halfHeight);
        }
    }

    @FunctionalInterface
    public interface TubeAnimSampler {
        float sample(int segIndex, float animTime);
    }
}