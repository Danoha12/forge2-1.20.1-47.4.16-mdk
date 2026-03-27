package com.trolmastercard.sexmod.client.render;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.RgbColor;
import com.trolmastercard.sexmod.util.VectorRotateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Arrays;

/**
 * NpcBoneQuadBuilder - ported from af.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Utility class that builds world-space quad meshes ({@code Vec3[][]}) around
 * named NPC bones, used to render the sex-position collision/interaction volumes.
 *
 * Two mesh shapes are supported:
 *
 *  <ul>
 *    <li><b>Box</b>  - 6 faces - 4 vertices (8-corner AABB) - used for 2-bone poses.
 *        Built by {@link #buildBoxFaces(Vec3[])} from 8 corner points.</li>
 *    <li><b>Capsule</b> - 10 faces - 4 vertices (two connected boxes) - used for
 *        3-bone poses.  Built by {@link #buildCapsuleFaces(Vec3[])} from 12 corner points.</li>
 *  </ul>
 *
 * Public entry points:
 *  <ul>
 *    <li>{@link #buildCapsuleQuads} - 3-bone capsule mesh (blowjob / anal scenes)</li>
 *    <li>{@link #buildBoxQuads}     - 2-bone box mesh (mating-press / basic scenes)</li>
 *    <li>{@link #renderQuads}       - emit all quads into a {@link BufferBuilder}</li>
 *    <li>{@link #applyNpcTranslation} - translate the PoseStack to the NPC's position</li>
 *  </ul>
 *
 * Obfuscation mapping:
 *   af.a(em,float,String,String,String,float,float,float,float,String) - buildCapsuleQuads(...)
 *   af.a(em,float,String,String,f7,f7)                                 - buildBoxQuads(...)
 *   af.b(em,float,String,String,f7,f7)       [internal]                - computeBoxCorners(...)
 *   af.b(em,float,String,String,String,f,f,f,f,String) [internal]      - computeCapsuleCorners(...)
 *   af.a(Vec3d[])                                                       - buildCapsuleFaces(...)
 *   af.b(Vec3d[])                                                       - buildBoxFaces(...)
 *   af.a(BufferBuilder,Vec3d[][],gv)                                    - renderQuads(...)
 *   af.a(Minecraft,em,float)                                            - applyNpcTranslation(...)
 *
 * New class introduced:
 *   {@code gv} - {@link RgbaColor}  (4-channel RGBA colour used when emitting vertices)
 *   {@code ck} - {@link VectorRotateUtil} (static Vec3 rotation helpers)
 */
@OnlyIn(Dist.CLIENT)
public final class NpcBoneQuadBuilder {

    private NpcBoneQuadBuilder() {}

    // =========================================================================
    //  Public API - build quads
    // =========================================================================

    /**
     * Builds a 10-face capsule mesh spanning three named bones on the NPC.
     *
     * The first box (faces 0-3 + cap 4) spans the ring around {@code bone1} and
     * the second box (faces 5-8 + cap 9) spans the ring around {@code bone3}, with
     * the reference bone {@code refBone} used to read the current Y/Z rotation so
     * the boxes are aligned to the animation's current pose.
     *
     * Parameters:
     *   {@code hw1/hd1}  - half-width and half-depth of the inner ring (bone1)
     *   {@code hw2/hd2}  - half-width and half-depth of the outer ring (bone3)
     *
     * Equivalent to: {@code af.a(em, float, String, String, String, float, float, float, float, String)}
     */
    public static Vec3[][] buildCapsuleQuads(
            BaseNpcEntity npc, float partialTick,
            String bone1Name, String bone2Name, String bone3Name,
            float hw1, float hd1, float hw2, float hd2,
            String refBoneName) {

        Vec3[] corners = computeCapsuleCorners(
            npc, partialTick,
            bone1Name, bone2Name, bone3Name,
            hw1, hd1, hw2, hd2,
            refBoneName);
        return buildCapsuleFaces(corners);
    }

    /**
     * Builds a 6-face box mesh spanning two named bones on the NPC.
     *
     * The shape of each box half is determined by the {@link RgbColor} extents:
     *  <ul>
     *    <li>If {@code ext1.r == 0 && ext1.g == 0} - flat Y/Z-plane box
     *        (corners at -ext.b on Z, -ext.r on Y, X=0)</li>
     *    <li>Otherwise - flat X/Y-plane box (corners at -ext.r on X, -ext.b on Y, Z=0)</li>
     *  </ul>
     *
     * Equivalent to: {@code af.a(em, float, String, String, f7, f7)}
     *
     * @param ext1  extents for the {@code bone1} half of the box  (f7 paramf71)
     * @param ext2  extents for the {@code bone2} half of the box  (f7 paramf72)
     */
    public static Vec3[][] buildBoxQuads(
            BaseNpcEntity npc, float partialTick,
            String bone1Name, String bone2Name,
            RgbColor ext1, RgbColor ext2) {

        Vec3[] corners = computeBoxCorners(npc, partialTick, bone1Name, bone2Name, ext1, ext2);
        return buildBoxFaces(corners);
    }

    // =========================================================================
    //  Internal - corner computation
    // =========================================================================

    /**
     * Computes 12 world-space corners for the capsule mesh.
     *
     * Layout:
     *   corners[0-3]  - ring around bone1  (inner cap)
     *   corners[4-7]  - ring shared between bone1 cap and bone2 segment
     *   corners[8-11] - ring around bone3  (outer cap)
     *
     * The {@code refBone} bone provides Y and Z rotation angles so the rings
     * stay aligned with the animated model.
     *
     * Equivalent to: {@code af.b(em, float, String, String, String, float, float, float, float, String)}
     */
    static Vec3[] computeCapsuleCorners(
            BaseNpcEntity npc, float partialTick,
            String bone1Name, String bone2Name, String bone3Name,
            float hw1, float hd1, float hw2, float hd2,
            String refBoneName) {

        // Get the reference bone's current rotation
        var refBone = npc.getAnimModel().getBone(refBoneName);
        if (refBone == null) {
            Vec3[] zero = new Vec3[12];
            Arrays.fill(zero, Vec3.ZERO);
            return zero;
        }
        float boneRotY = ItemRenderUtil.boneRotToDegrees(refBone.getRotationY());
        float boneRotZ = ItemRenderUtil.boneRotToDegrees(refBone.getRotationZ());

        // World-space positions of the three bones
        Vec3 pos1 = npc.getBonePosition(bone1Name);
        Vec3 pos2 = npc.getBonePosition(bone2Name);
        Vec3 pos3 = npc.getBonePosition(bone3Name);

        // Build 12 local-space corners:
        //   [0-3]  ring at hw1/hd1, centred on bone1 - XZ plane
        //   [4-7]  ring at hw1/hd1, centred on bone2 - XZ plane
        //   [8-11] ring at hw2/hd2, centred on bone3 - XZ plane
        Vec3[] corners = new Vec3[12];
        corners[0]  = new Vec3( hw1, 0.0,  -hd1);
        corners[1]  = new Vec3(-hw1, 0.0,  -hd1);
        corners[2]  = new Vec3(-hw1, 0.0,   hd1);
        corners[3]  = new Vec3( hw1, 0.0,   hd1);
        corners[4]  = new Vec3( hw1, hd1,   0.0);
        corners[5]  = new Vec3(-hw1, hd1,   0.0);
        corners[6]  = new Vec3(-hw1, -hd1,  0.0);
        corners[7]  = new Vec3( hw1, -hd1,  0.0);
        corners[8]  = new Vec3( hw2, 0.0,  -hd2);
        corners[9]  = new Vec3(-hw2, 0.0,  -hd2);
        corners[10] = new Vec3(-hw2, 0.0,   hd2);
        corners[11] = new Vec3( hw2, 0.0,   hd2);

        // Apply entity yaw rotation to all 12 corners
        for (int i = 0; i < corners.length; i++) {
            corners[i] = VectorRotateUtil.rotateY(corners[i], partialTick);
        }

        // Apply bone Y/Z rotation to the inner 4 corners (the cap ring)
        for (int i = 0; i < 4; i++) {
            corners[i] = VectorRotateUtil.rotateEuler(corners[i], 0.0F, boneRotY, boneRotZ);
        }

        // Translate each group to their respective bone positions
        for (int i = 0; i < 4; i++) {
            corners[i] = corners[i].add(pos1);
        }
        for (int i = 4; i < 8; i++) {
            corners[i] = corners[i].add(pos2);
        }
        for (int i = 8; i < 12; i++) {
            corners[i] = corners[i].add(pos3);
        }
        return corners;
    }

    /**
     * Computes 8 world-space corners for the two-bone box mesh.
     *
     * If {@code ext1.r == 0 && ext1.g == 0} (the "Y/Z-plane" branch):
     *   corners sit on the Y/Z plane (X = 0), -ext.b on Z, -ext.r on Y.
     * Otherwise (the "X/Y-plane" branch):
     *   corners sit on the X/Y plane (Z = 0), -ext.r on X, -ext.b on Y.
     *
     * After building, all 8 corners are rotated by the entity yaw, then the
     * first 4 are translated to bone1 and the last 4 to bone2.
     *
     * Equivalent to: {@code af.b(em, float, String, String, f7, f7)}
     *
     * @param ext1  extents for bone1's half  (fields: r=halfX, g=halfY, b=halfZ)
     * @param ext2  extents for bone2's half  (fields: r=halfX, g=halfY, b=halfZ)
     */
    static Vec3[] computeBoxCorners(
            BaseNpcEntity npc, float partialTick,
            String bone1Name, String bone2Name,
            RgbColor ext1, RgbColor ext2) {

        Vec3 pos1 = npc.getBonePosition(bone1Name);
        Vec3 pos2 = npc.getBonePosition(bone2Name);

        Vec3[] corners = new Vec3[8];

        if (ext1.r() == 0.0F && ext1.g() == 0.0F) {
            // Y/Z-plane variant (flat X=0 cross-section)
            // corners[0-3] for bone1
            corners[0] = new Vec3(0.0,  ext1.b(), ext1.g());
            corners[1] = new Vec3(0.0, -ext1.b(), ext1.g());
            corners[2] = new Vec3(0.0, -ext1.b(),-ext1.g());
            corners[3] = new Vec3(0.0,  ext1.b(),-ext1.g());
            // corners[4-7] for bone2
            corners[4] = new Vec3(0.0,  ext2.b(), ext2.g());
            corners[5] = new Vec3(0.0, -ext2.b(), ext2.g());
            corners[6] = new Vec3(0.0, -ext2.b(),-ext2.g());
            corners[7] = new Vec3(0.0,  ext2.b(),-ext2.g());
        } else {
            // X/Y-plane variant (flat Z=0 cross-section)
            // corners[0-3] for bone1
            corners[0] = new Vec3( ext1.r(),  ext1.b(), 0.0);
            corners[1] = new Vec3(-ext1.r(),  ext1.b(), 0.0);
            corners[2] = new Vec3(-ext1.r(), -ext1.b(), 0.0);
            corners[3] = new Vec3( ext1.r(), -ext1.b(), 0.0);
            // corners[4-7] for bone2
            corners[4] = new Vec3( ext2.r(),  ext2.b(), 0.0);
            corners[5] = new Vec3(-ext2.r(),  ext2.b(), 0.0);
            corners[6] = new Vec3(-ext2.r(), -ext2.b(), 0.0);
            corners[7] = new Vec3( ext2.r(), -ext2.b(), 0.0);
        }

        // Rotate all by entity yaw
        for (int i = 0; i < corners.length; i++) {
            corners[i] = VectorRotateUtil.rotateY(corners[i], partialTick);
        }
        // Translate to bone positions
        for (int i = 0; i < 4; i++) corners[i] = corners[i].add(pos1);
        for (int i = 4; i < 8; i++) corners[i] = corners[i].add(pos2);

        return corners;
    }

    // =========================================================================
    //  Face assembly - Capsule (10 faces from 12 corners)
    // =========================================================================

    /**
     * Assembles 10 quad faces from 12 corner points.
     *
     * The 12 corners form a capsule shape (two annular rings connected):
     * <pre>
     *   Face 0  - front  cap  inner:  0,1,5,4
     *   Face 1  - back   cap  inner:  1,2,6,5
     *   Face 2  - right  cap  inner:  3,2,6,7
     *   Face 3  - left   cap  inner:  0,4,7,3
     *   Face 4  - bottom cap  inner:  0,1,2,3
     *   Face 5  - front  cap  outer:  4,5,9,8
     *   Face 6  - right  outer ring:  9,10,6,5
     *   Face 7  - back   outer ring: 10,11,7,6
     *   Face 8  - left   outer ring:  4,7,11,8
     *   Face 9  - top    cap  outer:  8,9,10,11
     * </pre>
     *
     * Equivalent to: {@code af.a(Vec3d[])}
     */
    static Vec3[][] buildCapsuleFaces(Vec3[] c) {
        Vec3[][] faces = new Vec3[10][4];
        faces[0] = new Vec3[]{ c[0], c[1], c[5], c[4] };
        faces[1] = new Vec3[]{ c[1], c[2], c[6], c[5] };
        faces[2] = new Vec3[]{ c[3], c[2], c[6], c[7] };
        faces[3] = new Vec3[]{ c[0], c[4], c[7], c[3] };
        faces[4] = new Vec3[]{ c[0], c[1], c[2], c[3] };
        faces[5] = new Vec3[]{ c[4], c[5], c[9], c[8] };
        faces[6] = new Vec3[]{ c[9], c[10], c[6], c[5] };
        faces[7] = new Vec3[]{ c[10], c[11], c[7], c[6] };
        faces[8] = new Vec3[]{ c[4], c[7], c[11], c[8] };
        faces[9] = new Vec3[]{ c[8], c[9], c[10], c[11] };
        return faces;
    }

    // =========================================================================
    //  Face assembly - Box (6 faces from 8 corners)
    // =========================================================================

    /**
     * Assembles 6 quad faces from 8 corner points.
     *
     * Corner layout (c[0-3] = bone1 ring, c[4-7] = bone2 ring):
     * <pre>
     *   Face 0  - bone1 cap:   0,1,2,3
     *   Face 1  - bone2 cap:   4,5,6,7
     *   Face 2  - side A:      1,2,6,5
     *   Face 3  - side B:      3,7,4,0
     *   Face 4  - side C:      1,0,4,5
     *   Face 5  - side D:      2,3,7,6
     * </pre>
     *
     * Equivalent to: {@code af.b(Vec3d[])}
     */
    static Vec3[][] buildBoxFaces(Vec3[] c) {
        Vec3[][] faces = new Vec3[6][4];
        faces[0] = new Vec3[]{ c[0], c[1], c[2], c[3] };
        faces[1] = new Vec3[]{ c[4], c[5], c[6], c[7] };
        faces[2] = new Vec3[]{ c[1], c[2], c[6], c[5] };
        faces[3] = new Vec3[]{ c[3], c[7], c[4], c[0] };
        faces[4] = new Vec3[]{ c[1], c[0], c[4], c[5] };
        faces[5] = new Vec3[]{ c[2], c[3], c[7], c[6] };
        return faces;
    }

    // =========================================================================
    //  Rendering
    // =========================================================================

    /**
     * Emits all quad vertices into a {@link BufferBuilder}.
     *
     * Each corner vertex is emitted with UV (0, 0) and the provided RGBA colour.
     * The caller is responsible for begin/end on the buffer and for calling
     * {@code RenderSystem.setShader} and enabling the correct draw mode
     * ({@code GL_QUADS} or {@code VertexFormat.Mode.QUADS}).
     *
     * Equivalent to: {@code af.a(BufferBuilder, Vec3d[][], gv)}
     *
     * @param builder  the buffer to emit into
     * @param quads    face array produced by {@link #buildCapsuleFaces} or {@link #buildBoxFaces}
     * @param color    RGBA colour to apply to every vertex
     */
    public static void renderQuads(BufferBuilder builder, Vec3[][] quads, RgbaColor color) {
        for (Vec3[] face : quads) {
            for (Vec3 v : face) {
                // 1.20.1 BufferBuilder chain: vertex - uv - color - endVertex
                builder.vertex(v.x, v.y, v.z)
                       .uv(0.0F, 0.0F)
                       .color(color.r(), color.g(), color.b(), color.a())
                       .endVertex();
            }
        }
    }

    // =========================================================================
    //  GL / PoseStack positioning
    // =========================================================================

    /**
     * Translates {@code poseStack} so that subsequent rendering is expressed
     * relative to the NPC entity's interpolated world position.
     *
     * In 1.12.2, this method modified the global GL matrix directly via
     * {@code GlStateManager.translate}. In 1.20.1 the global matrix is gone;
     * all transforms are done via the {@link PoseStack} passed into every
     * render call.
     *
     * Steps:
     *  1. Translate to {@code (0, 0.01, 0)} (small Y offset to avoid Z-fighting)
     *  2. Determine the NPC's current interpolated position
     *  3. Determine the local player's current interpolated position
     *  4. Compute the offset from the player to the NPC and apply it
     *     (because the render origin is always the camera/player in 1.20.1)
     *
     * Equivalent to: {@code af.a(Minecraft, em, float)}
     *
     * @param mc           the Minecraft instance
     * @param npc          the NPC entity whose mesh is about to be rendered
     * @param partialTick  partial-tick interpolation factor
     * @param poseStack    the current render PoseStack
     */
    public static void applyNpcTranslation(Minecraft mc, BaseNpcEntity npc,
                                            float partialTick, PoseStack poseStack) {
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Initial Y offset to avoid Z-fighting with block surfaces
        poseStack.translate(0.0, 0.01, 0.0);

        // Resolve the entity the NPC is rendering relative to (usually a sex partner)
        Entity relativeEntity = resolveRelativeEntity(mc, npc);

        // Interpolate entity position for this frame
        Vec3 npcPos;
        if (npc.isPositionLocked()) {
            npcPos = npc.getLockedPosition();
        } else {
            Vec3 npcPrev = new Vec3(relativeEntity.xo, relativeEntity.yo, relativeEntity.zo);
            npcPos = MathUtil.lerpPosition(npcPrev, relativeEntity.position(), partialTick);
        }

        // Interpolate player position
        Vec3 playerPrev  = new Vec3(player.xo, player.yo, player.zo);
        Vec3 playerPos   = MathUtil.lerpPosition(playerPrev, player.position(), partialTick);

        // Offset vector from player (camera origin) to the NPC
        Vec3 offset = npcPos.subtract(playerPos);

        // Apply the entity's local transform (e.g. yaw-corrected offset)
        offset = npc.transformRenderOffset(offset, partialTick);

        poseStack.translate(offset.x, offset.y, offset.z);
    }

    /**
     * Resolves the "relative entity" used as the NPC's position reference
     * for rendering - typically the sex partner entity.
     *
     * In 1.12.2 this cast into the NPC's custom renderer and called
     * {@code .c(npc)} to get the partner.  In 1.20.1 we call a method on the
     * NPC directly instead.
     *
     * TODO: Replace with {@code npc.getSexPartnerEntity()} once BaseNpcEntity
     *       exposes that method, or pull from the renderer via
     *       {@code mc.getEntityRenderDispatcher().getRenderer(npc)}.
     */
    private static Entity resolveRelativeEntity(Minecraft mc, BaseNpcEntity npc) {
        // Fallback: use the NPC itself as the reference entity
        return npc;
    }

    // =========================================================================
    //  Inner type - RGBA colour  (replaces obfuscated "gv" class)
    // =========================================================================

    /**
     * RGBA colour record used when emitting vertices via {@link #renderQuads}.
     *
     * Field mapping from the original {@code gv} class:
     *   {@code gv.a} = r,  {@code gv.d} = g,  {@code gv.c} = b,  {@code gv.b} = a
     *
     * Note: the original vertex call was {@code color(gv.a, gv.d, gv.c, gv.b)},
     * so the field-to-channel mapping was:  a-R, d-G, c-B, b-A.
     */
    public record RgbaColor(float r, float g, float b, float a) {

        /** Opaque white. */
        public static final RgbaColor WHITE = new RgbaColor(1.0F, 1.0F, 1.0F, 1.0F);
        /** Fully transparent. */
        public static final RgbaColor CLEAR = new RgbaColor(0.0F, 0.0F, 0.0F, 0.0F);
    }
}
