package com.trolmastercard.sexmod.util;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.GeoBone;

/**
 * BoneMatrixUtil — Portado a 1.20.1 y optimizado para JOML.
 * * Utilidad para extraer la matriz de modelo de un hueso de GeckoLib 4 y
 * aplicarla al pipeline de renderizado de Minecraft.
 * Permite posicionar objetos (como ítems en la mano) basándose en la
 * transformación acumulada del hueso y su punto de pivote.
 */
public final class BoneMatrixUtil {

  private BoneMatrixUtil() {}

  // =========================================================================
  //  API Pública
  // =========================================================================

  /**
   * Extrae la matriz actual del PoseStack, aplica la transposición necesaria
   * para el pipeline de la GPU y traslada el origen al punto de pivote del hueso.
   * * @param boneStack El PoseStack que contiene la transformación acumulada.
   * @param bone      El hueso de GeckoLib cuyo pivote queremos usar como referencia.
   */
  public static void applyBoneMatrix(PoseStack boneStack, GeoBone bone) {
    // Obtenemos la matriz de pose actual (JOML Matrix4f)
    Matrix4f model = new Matrix4f(boneStack.last().pose());

    // Transponer para convertir de Column-Major a Row-Major (estándar de shaders)
    model.transpose();

    // Trasladar al pivote del hueso.
    // Nota: Dividimos entre 16.0F para convertir de píxeles (Blockbench) a bloques (Minecraft).
    boneStack.translate(
            bone.getPivotX() / 16.0F,
            bone.getPivotY() / 16.0F,
            bone.getPivotZ() / 16.0F
    );
  }

  // =========================================================================
  //  Helpers de Matrices (JOML)
  // =========================================================================

  /**
   * Escribe los 16 elementos de la matriz en un arreglo plano de floats.
   * Útil para pasar datos a buffers de OpenGL antiguos si fuera necesario.
   */
  public static void matrixToArray(float[] dest, Matrix4f src) {
    if (dest.length < 16) return;

    dest[ 0] = src.m00(); dest[ 1] = src.m01(); dest[ 2] = src.m02(); dest[ 3] = src.m03();
    dest[ 4] = src.m10(); dest[ 5] = src.m11(); dest[ 6] = src.m12(); dest[ 7] = src.m13();
    dest[ 8] = src.m20(); dest[ 9] = src.m21(); dest[10] = src.m22(); dest[11] = src.m23();
    dest[12] = src.m30(); dest[13] = src.m31(); dest[14] = src.m32(); dest[15] = src.m33();
  }

  /**
   * Realiza la multiplicación de matrices: (Right * Left).
   * En el pipeline de transformación, esto aplica la matriz 'Left' primero.
   */
  public static Matrix4f multiply(Matrix4f left, Matrix4f right) {
    // JOML mul() modifica la instancia, por eso creamos una copia de 'right'
    return new Matrix4f(right).mul(left);
  }
}