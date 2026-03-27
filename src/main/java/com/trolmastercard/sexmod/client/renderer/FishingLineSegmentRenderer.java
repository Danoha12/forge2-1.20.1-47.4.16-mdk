package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * FishingLineSegmentRenderer - ported from ef.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Renders a ribbon-shaped fishing line made of quad segments. Each segment is a
 * flat quad constructed from 4 vertices. The ribbon can twist, bend and undulate
 * based on time-driven sine offsets.
 *
 * Public API: {@link #render(BufferBuilder, Minecraft, Config)}
 *
 * The inner {@link Config} class (original {@code b}) carries all parameters for
 * one ribbon draw call: color, segment count, width/height, initial direction
 * angle, and three {@link Modifier} lambdas (x/y/z per-segment offsets).
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - BufferBuilder.func_181668_a(7, QUADS_COLOR) - builder.begin(QUADS, POSITION_COLOR)
 *   - DefaultVertexFormats.field_181706_f - DefaultVertexFormat.POSITION_COLOR
 *   - func_181662_b(x,y,z) - vertex(x,y,z)
 *   - func_181669_b(a,b,g,r) - color(r,g,b,a) [note: original stores rgba as a,d,c,b]
 *   - func_181675_d() - endVertex()
 *   - Tessellator.func_78381_a() - Tesselator.getInstance().end()
 *   - func_181668_a without Tessellator.begin - builder.begin(QUADS, format) only
 *   - Vec3d - Vec3; field_72450_a/b/c - x/y/z
 *   - ck.a(direction.normalize(), angle) - VectorMathUtil.rotateAroundY(dir, angle)
 *   - Minecraft.field_71439_g.field_70173_aa - mc.player.tickCount
 *   - func_184121_ak() - mc.getFrameTime()
 *   - gv - RgbaColor (fields a=alpha, b=red?, c=green?, d=blue? - preserved as-is from original)
 */
public class FishingLineSegmentRenderer {

    // -- Public render ----------------------------------------------------------

    /**
     * Renders a ribbon-shaped fishing line segment.
     *
     * @param builder  the active BufferBuilder (should be in POSITION_COLOR mode)
     * @param mc       Minecraft instance (for current time)
     * @param config   all parameters for this ribbon
     */
    public static void render(BufferBuilder builder, Minecraft mc, Config config) {
        Vec3[] quad = {
            new Vec3(-config.halfWidth, -config.halfHeight, 0.0),
            new Vec3(-config.halfWidth,  config.halfHeight, 0.0),
            new Vec3( config.halfWidth,  config.halfHeight, 0.0),
            new Vec3( config.halfWidth, -config.halfHeight, 0.0)
        };

        // Initial "forward" direction along -Z, rotated by the config angle
        Vec3 forward = new Vec3(0.0, 0.0, -config.initialLength);
        Vec3 cursor  = VectorMathUtil.rotateAroundY(forward.normalize(), config.initialAngle);

        // Build segment ring positions
        Vec3[] prevRing = new Vec3[4];
        System.arraycopy(applyAngle(quad, cursor), 0, prevRing, 0, 4);

        java.util.List<Vec3[]> rings = new java.util.ArrayList<>();
        float time = mc.player != null
                ? mc.player.tickCount + mc.getFrameTime()
                : 0.0F;

        for (int seg = 0; seg <= config.segmentCount; seg++) {
            float t = 1.0F - (float) seg / config.segmentCount;
            Vec3[] ring = new Vec3[4];
            for (int v = 0; v < 4; v++) {
                Vec3 base = quad[v];
                ring[v] = new Vec3(base.x * t, base.y, base.z).add(cursor);
            }
            rings.add(ring);

            // Advance cursor by per-segment offset
            forward = VectorMathUtil.rotateAroundY(
                    VectorMathUtil.applyOffset(forward,
                            config.xModifier.compute(seg, time),
                            config.yModifier.compute(seg, time),
                            config.zModifier.compute(seg, time)),
                    config.initialAngle);
            cursor = cursor.add(forward);
        }

        // Emit quads
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        emitQuads(builder, prevRing, rings.get(0), config.color);
        for (int i = 0; i < config.segmentCount - 1; i++) {
            emitQuads(builder, rings.get(i), rings.get(i + 1), config.color);
        }
        builder.end();
    }

    // -- Sine helper ------------------------------------------------------------

    /**
     * Computes a sine-based wave value.
     *
     * {@code sin(speed * freq + phase * segIndex) * amplitude}
     */
    public static float sineWave(float speed, float freq, float phase, int segIndex, float amplitude) {
        return (float) (Math.sin(speed * freq + phase * segIndex) * amplitude);
    }

    // -- Quad emission ----------------------------------------------------------

    private static void emitQuads(BufferBuilder builder, Vec3[] a, Vec3[] b, RgbaColor color) {
        // Front face
        putVertex(builder, a[1], color); putVertex(builder, a[2], color);
        putVertex(builder, b[2], color); putVertex(builder, b[1], color);
        // Back face
        putVertex(builder, a[0], color); putVertex(builder, a[1], color);
        putVertex(builder, b[1], color); putVertex(builder, b[0], color);
        // Right face
        putVertex(builder, a[2], color); putVertex(builder, a[3], color);
        putVertex(builder, b[3], color); putVertex(builder, b[2], color);
        // Left face
        putVertex(builder, a[0], color); putVertex(builder, a[3], color);
        putVertex(builder, b[3], color); putVertex(builder, b[0], color);
    }

    private static void putVertex(BufferBuilder builder, Vec3 v, RgbaColor color) {
        builder.vertex(v.x, v.y, v.z)
               .color(color.r, color.g, color.b, color.a)
               .endVertex();
    }

    private static Vec3[] applyAngle(Vec3[] quad, Vec3 dir) {
        Vec3[] out = new Vec3[4];
        for (int i = 0; i < 4; i++) out[i] = quad[i].add(dir);
        return out;
    }

    // -- Config (inner class, mirrors original ef.b) ----------------------------

    /**
     * All parameters needed to render one fishing-line ribbon.
     */
    public static class Config {
        /** RGBA color of the ribbon. */
        public final RgbaColor color;
        /** Initial direction angle (degrees). */
        public final float     initialAngle;
        /** Number of ribbon segments. */
        public final int       segmentCount;
        /** Length of the initial forward vector. */
        public final float     initialLength;
        /** Per-segment X offset modifier. */
        public final Modifier  xModifier;
        /** Per-segment Y offset modifier. */
        public final Modifier  yModifier;
        /** Per-segment Z offset modifier. */
        public final Modifier  zModifier;
        /** Half-width of each ribbon quad. */
        public final float     halfWidth;
        /** Half-height of each ribbon quad. */
        public final float     halfHeight;

        public Config(RgbaColor color, float initialAngle, int segmentCount,
                      float initialLength,
                      Modifier xMod, Modifier yMod, Modifier zMod,
                      float halfWidth, float halfHeight) {
            this.color         = color;
            this.initialAngle  = initialAngle;
            this.segmentCount  = segmentCount;
            this.initialLength = initialLength;
            this.xModifier     = xMod;
            this.yModifier     = yMod;
            this.zModifier     = zMod;
            this.halfWidth     = halfWidth;
            this.halfHeight    = halfHeight;
        }

        /** Returns a copy of this config. */
        public Config copy() {
            return new Config(color, initialAngle, segmentCount, initialLength,
                    xModifier, yModifier, zModifier, halfWidth, halfHeight);
        }
    }

    // -- Modifier functional interface (mirrors original ef.a) -----------------

    /** Time-driven per-segment float modifier. */
    @FunctionalInterface
    public interface Modifier {
        /**
         * Compute the modifier value for segment {@code index} at time {@code time}.
         */
        float compute(int index, float time);
    }
}
