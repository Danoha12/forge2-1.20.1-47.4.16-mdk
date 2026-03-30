package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.EnergyBallEntity;
import com.trolmastercard.sexmod.util.RgbColor;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/**
 * EnergyBallRenderer — Portado a 1.20.1.
 * * Renderiza el proyectil (EnergyBall) como un Quad 2D que siempre mira a la cámara.
 */
@OnlyIn(Dist.CLIENT)
public class EnergyBallRenderer extends EntityRenderer<EnergyBallEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/entity/galath/energy_ball.png");

    private static final RgbColor COLOR_CYAN = new RgbColor(0, 255, 251);
    private static final RgbColor COLOR_MAGENTA = new RgbColor(255, 0, 236);
    private static final RgbColor COLOR_WHITE = new RgbColor(255, 255, 255);

    public EnergyBallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyBallEntity entity) {
        return TEXTURE;
    }

    // ── Renderizado (Billboard Quad) ─────────────────────────────────────────

    @Override
    public void render(EnergyBallEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // 1. Hacer que el plano mire siempre a la cámara (Billboard)
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F)); // Corrección de orientación para texturas

        // 2. Escalar según la carga (Charge)
        float charge = (float) entity.getCharge();
        poseStack.scale(charge, charge, charge);

        // 3. Calcular colores interpolados
        RgbColor col1, col2;
        if (charge >= 1.0F) {
            float time = entity.level().getGameTime() + partialTick;
            double t = 0.5 * Math.sin(time * 0.5) + 0.5;
            col1 = lerpColor(COLOR_CYAN, COLOR_MAGENTA, t);
            col2 = lerpColor(COLOR_MAGENTA, COLOR_CYAN, t);
        } else {
            col1 = lerpColor(COLOR_WHITE, COLOR_CYAN, charge);
            col2 = col1;
        }

        // 4. Preparar OpenGL State
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        Matrix4f pose = poseStack.last().pose(); // Extraemos la matriz de posición actual

        // 5. Dibujar Quad Principal
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitQuad(buf, pose, col1, 0.0F);
        tesselator.end();

        // 6. Dibujar Quad Secundario (más pequeño)
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0D, 0.075D, 0.001D); // Desplazamiento Z ligero para evitar Z-Fighting
        pose = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitQuad(buf, pose, col2, 0.001F);
        tesselator.end();

        // 7. Limpiar OpenGL State
        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Dibuja el Quad en la matriz especificada. */
    private void emitQuad(BufferBuilder buf, Matrix4f pose, RgbColor col, float zOff) {
        int r = col.r(), g = col.g(), b = col.b();
        // El orden de los vértices es VITAL en 1.20.1 para que la textura no se rompa
        buf.vertex(pose, -0.25F, 0.0F, zOff).color(r, g, b, 255).uv(0.0F, 1.0F).endVertex();
        buf.vertex(pose,  0.25F, 0.0F, zOff).color(r, g, b, 255).uv(1.0F, 1.0F).endVertex();
        buf.vertex(pose,  0.25F, 0.5F, zOff).color(r, g, b, 255).uv(1.0F, 0.0F).endVertex();
        buf.vertex(pose, -0.25F, 0.5F, zOff).color(r, g, b, 255).uv(0.0F, 0.0F).endVertex();
    }

    private static RgbColor lerpColor(RgbColor a, RgbColor b, double t) {
        return new RgbColor(
                (int) (a.r() + (b.r() - a.r()) * t),
                (int) (a.g() + (b.g() - a.g()) * t),
                (int) (a.b() + (b.b() - a.b()) * t)
        );
    }
}