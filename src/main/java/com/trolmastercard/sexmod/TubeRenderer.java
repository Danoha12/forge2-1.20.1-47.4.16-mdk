package com.trolmastercard.sexmod;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

/**
 * TubeRenderer - ported from ef.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Renders a segmented tube (ribbon quad-strip) along a bezier-like curved path.
 * Used for fishing lines, tentacles, and similar flexible connection effects.
 *
 * The tube is built by:
 *   1. Generating a series of cross-section quad frames.
 *   2. Walking a direction vector ({@code vec3d1}) that is perturbed each step
 *      by yaw/pitch/roll offsets (from {@link TubeConfig} lambda fields).
 *   3. Connecting successive frames with 4 quad faces (top, bottom, left, right).
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - BufferBuilder.func_181668_a(7, DefaultVertexFormats.field_181706_f) -
 *       bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)
 *   - func_181662_b(x,y,z).func_181669_b(a,d,c,b).func_181675_d() -
 *       vertex(x,y,z).color(r,g,b,a).endVertex()
 *   - tessellator.func_78381_a() - tessellator.end()
 *   - DefaultVertexFormats.field_181706_f - DefaultVertexFormat.POSITION_COLOR
 *   - Vec3d - Vec3; field_72450_a/field_72448_b/field_72449_c - x/y/z
 *   - func_72432_b() - normalize()
 *   - func_178787_e() - add()
 *   - Minecraft.field_71439_g.field_70173_aa - player.tickCount
 *   - func_184121_ak() - getPartialTick() (or pass partialTick externally)
 *   - ck.a(vec, euler) - VectorMathUtil.rotateEuler(vec, euler)
 *   - ck.a(vel, yaw, pitch, roll) - VectorMathUtil.perturbDirection(vel, yaw, pitch, roll)
 *   - gv - RgbaColor
 */
public class TubeRenderer {

    // -- Public entry point -----------------------------------------------------

    /**
     * Renders a tube into {@code bufferBuilder} according to {@code config}.
     *
     * @param bufferBuilder destination buffer (already begun or managed by caller)
     * @param mc            Minecraft instance (for animation time)
     * @param config        all tube parameters
     */
    public static void render(BufferBuilder bufferBuilder, Minecraft mc, TubeConfig config) {
        // Initial cross-section corners (w - h rectangle in XY, pointing -Z)
        Vec3[] crossSection = {
                new Vec3(-config.halfWidth, -config.halfHeight, 0.0D),
                new Vec3(-config.halfWidth,  config.halfHeight, 0.0D),
                new Vec3( config.halfWidth,  config.halfHeight, 0.0D),
                new Vec3( config.halfWidth, -config.halfHeight, 0.0D)
        };

        // Direction the tube points (initially straight -Z)
        Vec3 dir      = new Vec3(0.0D, 0.0D, -config.length);
        // Rotate initial direction by the tube's starting euler angle
        Vec3 dirNorm  = VectorMathUtil.rotateEuler(dir.normalize(), config.startAngle);
        // Running world-space offset
        Vec3 offset   = dirNorm;

        // Accumulate frames
        ArrayList<Vec3[]> frames = new ArrayList<>();
        float animTime = mc.player != null
                ? mc.player.tickCount + mc.getFrameTime()
                : 0.0F;

        // Build first frame (at origin)
        Vec3[] firstFrame = buildFrame(crossSection, 0, config, dirNorm);
        frames.add(firstFrame);

        for (int seg = 0; seg <= config.segments; seg++) {
            Vec3[] frame = buildFrame(crossSection, seg, config, dirNorm);
            frames.add(frame);

            // Perturb direction for next segment
            dir    = VectorMathUtil.perturbDirection(
                    dir,
                    config.yawOffset.sample(seg, animTime),
                    config.pitchOffset.sample(seg, animTime),
                    config.rollOffset.sample(seg, animTime));
            dirNorm = dirNorm.add(dir);
        }

        // Render quad strip
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        writeQuad(bufferBuilder, firstFrame, frames.get(0), config.color);
        for (int seg = 0; seg < config.segments - 1; seg++) {
            writeQuad(bufferBuilder, frames.get(seg), frames.get(seg + 1), config.color);
        }

        bufferBuilder.end();
    }

    // -- Helpers ----------------------------------------------------------------

    /** Builds a cross-section frame at segment {@code seg}, tapering width with (1 - seg/segments). */
    private static Vec3[] buildFrame(Vec3[] crossSection, int seg, TubeConfig cfg, Vec3 dirNorm) {
        float taper = 1.0F - (float) seg / cfg.segments;
        Vec3[] frame = new Vec3[4];
        for (int i = 0; i < 4; i++) {
            Vec3 c = crossSection[i];
            frame[i] = new Vec3(c.x * taper, c.y, c.z).add(dirNorm);
        }
        return frame;
    }

    /**
     * Writes 4 quads (top, bottom, left, right faces) connecting two cross-section frames.
     * Mirrors original 16-vertex quad-strip layout (4 faces - 4 vertices).
     */
    static void writeQuad(BufferBuilder buf, Vec3[] from, Vec3[] to, RgbaColor color) {
        int r = color.r, g = color.g, b = color.b, a = color.a;

        // Top face
        vert(buf, from[1], r, g, b, a);
        vert(buf, from[2], r, g, b, a);
        vert(buf,   to[2], r, g, b, a);
        vert(buf,   to[1], r, g, b, a);

        // Bottom face
        vert(buf, from[0], r, g, b, a);
        vert(buf, from[1], r, g, b, a);
        vert(buf,   to[1], r, g, b, a);
        vert(buf,   to[0], r, g, b, a);

        // Right face
        vert(buf, from[2], r, g, b, a);
        vert(buf, from[3], r, g, b, a);
        vert(buf,   to[3], r, g, b, a);
        vert(buf,   to[2], r, g, b, a);

        // Left face
        vert(buf, from[0], r, g, b, a);
        vert(buf, from[3], r, g, b, a);
        vert(buf,   to[3], r, g, b, a);
        vert(buf,   to[0], r, g, b, a);
    }

    private static void vert(BufferBuilder buf, Vec3 v, int r, int g, int b, int a) {
        buf.vertex(v.x, v.y, v.z).color(r, g, b, a).endVertex();
    }

    // -- Sine wave helper (mirrors original static a(float,float,float,int,float)) --

    public static float sinWave(float time, float freq, float phase, int seg, float amp) {
        return (float) (Math.sin(time * freq + phase * seg) * amp);
    }

    // -- Config class (mirrors inner class ef.b) --------------------------------

    /**
     * Configuration for a tube render call.
     *
     *   color       - RGBA color of the tube
     *   startAngle  - starting euler Y-angle in degrees
     *   segments    - number of tube segments
     *   length      - tube length (magnitude of initial direction vector)
     *   yawOffset   - per-segment yaw perturbation (TubeAnimSampler functional interface)
     *   pitchOffset - per-segment pitch perturbation
     *   rollOffset  - per-segment roll perturbation
     *   halfWidth   - half-width of cross-section rectangle
     *   halfHeight  - half-height of cross-section rectangle
     */
    public static class TubeConfig {
        public RgbaColor    color;
        public float        startAngle;
        public int          segments;
        public float        length;
        public TubeAnimSampler yawOffset;
        public TubeAnimSampler pitchOffset;
        public TubeAnimSampler rollOffset;
        public float        halfWidth;
        public float        halfHeight;

        public TubeConfig(RgbaColor color, float startAngle, int segments, float length,
                          TubeAnimSampler yaw, TubeAnimSampler pitch, TubeAnimSampler roll,
                          float halfWidth, float halfHeight) {
            this.color       = color;
            this.startAngle  = startAngle;
            this.segments    = segments;
            this.length      = length;
            this.yawOffset   = yaw;
            this.pitchOffset = pitch;
            this.rollOffset  = roll;
            this.halfWidth   = halfWidth;
            this.halfHeight  = halfHeight;
        }

        /** Returns a copy of this config. */
        public TubeConfig copy() {
            return new TubeConfig(color, startAngle, segments, length,
                    yawOffset, pitchOffset, rollOffset, halfWidth, halfHeight);
        }
    }

    /** Functional interface for per-segment animation sampling (mirrors ef.a). */
    @FunctionalInterface
    public interface TubeAnimSampler {
        float sample(int segIndex, float animTime);
    }
}
