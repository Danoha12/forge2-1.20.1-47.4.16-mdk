package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete de renderizadores

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.item.WinchesterItem;
import com.trolmastercard.sexmod.client.model.WinchesterModel; // Asegúrate de tener este modelo
import com.trolmastercard.sexmod.util.DevToolsHandler;
import com.trolmastercard.sexmod.util.NpcWorldUtil;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * WinchesterRenderer — Portado a 1.20.1.
 * * GeoItemRenderer para el WinchesterItem.
 * * Aplica un pase de iluminación Lambertiana direccional personalizado
 * * cuando se activa a través de las herramientas de desarrollo.
 */
public class WinchesterRenderer extends GeoItemRenderer<WinchesterItem> {

    /** Vector 'Arriba' constante usado en el cálculo de luz Lambertiana. */
    private static final Vector3f UP = new Vector3f(0f, 1f, 0f);

    public WinchesterRenderer() {
        super(new WinchesterModel());
    }

    // ── Color por Hueso: Aplicar luz direccional personalizada ───────────────

    @Override
    public void renderCubesOfBone(PoseStack poseStack, GeoBone bone, VertexConsumer buffer,
                                  int packedLight, int packedOverlay,
                                  float red, float green, float blue, float alpha) {

        float litR = red, litG = green, litB = blue;
        int finalPackedLight = packedLight;

        float[] lightDir = DevToolsHandler.lightDir; // Asumo que esto es estático

        if (lightDir != null && lightDir[0] == 0f) {

            // 🚨 1.20.1: El equivalente a glDisable(GL_LIGHTING) es forzar el brillo máximo
            // para que Minecraft no aplique sus propias sombras de bloque/cielo sobre nuestro color.
            finalPackedLight = LightTexture.FULL_BRIGHT;

            var sexmodLight = NpcWorldUtil.getSexmodLightDir(); // Debería devolver Vector3f

            if (sexmodLight != null) {
                // Aproximación de normal plana (Nota: si quieres que la luz cambie al rotar el arma en la mano,
                // deberías transformar este vector UP por poseStack.last().normal() )
                Vector3f normal = new Vector3f(UP);

                // 🧮 JOML: Producto punto nativo max(0, N·L)
                float dot = Math.max(0f, normal.dot(sexmodLight));

                float ambient = 0.3f;
                float diffuse = 0.7f * dot;
                float light = ambient + diffuse;

                litR = Math.min(1f, red * light);
                litG = Math.min(1f, green * light);
                litB = Math.min(1f, blue * light);
            }
        }

        super.renderCubesOfBone(poseStack, bone, buffer, finalPackedLight, packedOverlay,
                litR, litG, litB, alpha);
    }
}