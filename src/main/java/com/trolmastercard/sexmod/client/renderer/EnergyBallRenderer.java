package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.EnergyBallEntity;
import com.trolmastercard.sexmod.util.RgbaColor; // 🚨 REPARADO: Importamos RgbaColor
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
 * REPARADO: Sincronizado para usar RgbaColor en lugar de RgbColor.
 */
@OnlyIn(Dist.CLIENT)
public class EnergyBallRenderer extends EntityRenderer<EnergyBallEntity> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/entity/galath/energy_ball.png");

    // 🚨 REPARADO: Ahora son RgbaColor
    private static final RgbaColor COLOR_CYAN = new RgbaColor(0, 255, 251);
    private static final RgbaColor COLOR_MAGENTA = new RgbaColor(255, 0, 236);
    private static final RgbaColor COLOR_WHITE = new RgbaColor(255, 255, 255);

    public EnergyBallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(EnergyBallEntity entity) {
        return TEXTURE;
    }

    @Override
    public void render(EnergyBallEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        float charge = (float) entity.getCharge();
        poseStack.scale(charge, charge, charge);

        // 🚨 REPARADO: Lógica de colores usando RgbaColor
        RgbaColor col1, col2;
        if (charge >= 1.0F) {
            float time = entity.level().getGameTime() + partialTick;
            float t = (float)(0.5 * Math.sin(time * 0.5) + 0.5);
            col1 = RgbaColor.lerp(COLOR_CYAN, COLOR_MAGENTA, t);
            col2 = RgbaColor.lerp(COLOR_MAGENTA, COLOR_CYAN, t);
        } else {
            col1 = RgbaColor.lerp(COLOR_WHITE, COLOR_CYAN, charge);
            col2 = col1;
        }

        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();
        Matrix4f pose = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitQuad(buf, pose, col1, 0.0F);
        tesselator.end();

        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0D, 0.075D, 0.001D);
        pose = poseStack.last().pose();

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        emitQuad(buf, pose, col2, 0.001F);
        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private void emitQuad(BufferBuilder buf, Matrix4f pose, RgbaColor col, float zOff) {
        // 🚨 REPARADO: Usamos los métodos r(), g(), b() de nuestra clase unificada
        int r = col.r();
        int g = col.g();
        int b = col.b();
        int a = col.a();

        buf.vertex(pose, -0.25F, 0.0F, zOff).color(r, g, b, a).uv(0.0F, 1.0F).endVertex();
        buf.vertex(pose,  0.25F, 0.0F, zOff).color(r, g, b, a).uv(1.0F, 1.0F).endVertex();
        buf.vertex(pose,  0.25F, 0.5F, zOff).color(r, g, b, a).uv(1.0F, 0.0F).endVertex();
        buf.vertex(pose, -0.25F, 0.5F, zOff).color(r, g, b, a).uv(0.0F, 0.0F).endVertex();
    }
}