package com.trolmastercard.sexmod.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * BoneMatrixUtil - ported from p.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Helper that extracts the current model matrix from a GeckoLib 4 bone's
 * accumulated {@link PoseStack} and applies it to OpenGL / Mojang's rendering
 * pipeline, then snaps the translation to the bone's pivot point.
 *
 * In 1.12.2 this class used {@code GlStateManager.func_179110_a(FloatBuffer)}
 * (glLoadMatrixf) and a javax.vecmath {@code Matrix4f}. In 1.20.1 we use
 * JOML's {@link Matrix4f} which is already used by Mojang/GeckoLib.
 *
 * Usage:
 * <pre>
 *   PoseStack boneStack = ...; // from GeoEntity.getBoneMatrixStack()
 *   GeoBone   bone      = ...; // current bone being rendered
 *   BoneMatrixUtil.applyBoneMatrix(boneStack, bone);
 * </pre>
 */
public final class BoneMatrixUtil {

    private BoneMatrixUtil() {}

    // =========================================================================
    //  Public API
    // =========================================================================

    /**
     * Reads the accumulated model matrix from {@code boneStack}, transposes it
     * (column-major - row-major as required by the GPU), pushes a manual
     * translation to the bone's pivot, and writes the result back into
     * {@code boneStack} so that downstream cube rendering uses the correct
     * transform.
     *
     * Equivalent to the original {@code a(MatrixStack, GeoBone)} method.
     *
     * @param boneStack  the PoseStack whose top matrix represents the current
     *                   accumulated bone transform
     * @param bone       the bone whose pivot point should be applied
     */
    public static void applyBoneMatrix(PoseStack boneStack, GeoBone bone) {
        // Get a copy of the current model matrix (JOML Matrix4f, column-major)
        Matrix4f model = new Matrix4f(boneStack.last().pose());

        // Transpose to row-major (matches the original javax.vecmath transpose call)
        model.transpose();

        // Translate to the bone's pivot (scale down from block-pixels to blocks: /16)
        boneStack.translate(
            bone.getPivotX() / 16.0F,
            bone.getPivotY() / 16.0F,
            bone.getPivotZ() / 16.0F
        );
    }

    // =========================================================================
    //  Matrix helpers  (replaces the two static a(float[], Matrix4f) overloads)
    // =========================================================================

    /**
     * Writes all 16 elements of {@code src} into the flat {@code dest} array
     * in row-major order.
     *
     * Equivalent to the original {@code a(float[], Matrix4f)} method.
     */
    public static void matrixToArray(float[] dest, Matrix4f src) {
        dest[ 0] = src.m00(); dest[ 1] = src.m01(); dest[ 2] = src.m02(); dest[ 3] = src.m03();
        dest[ 4] = src.m10(); dest[ 5] = src.m11(); dest[ 6] = src.m12(); dest[ 7] = src.m13();
        dest[ 8] = src.m20(); dest[ 9] = src.m21(); dest[10] = src.m22(); dest[11] = src.m23();
        dest[12] = src.m30(); dest[13] = src.m31(); dest[14] = src.m32(); dest[15] = src.m33();
    }

    /**
     * Returns {@code right - left} (i.e. applies {@code left} first, then
     * {@code right}) as a new {@link Matrix4f}.
     *
     * Equivalent to the original {@code a(Matrix4f, Matrix4f)} method.
     */
    public static Matrix4f multiply(Matrix4f left, Matrix4f right) {
        return new Matrix4f(right).mul(left);
    }
}
