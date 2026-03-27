package com.trolmastercard.sexmod.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.trolmastercard.sexmod.entity.EnergyBallEntity;
import com.trolmastercard.sexmod.util.MathUtil;
import com.trolmastercard.sexmod.util.RgbColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * EnergyBallRenderer - ported from ag.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Renders the galath "energy ball" projectile entity ({@code c4} - {@link EnergyBallEntity}).
 * The ball is drawn as a billboard quad (always facing the camera) with an
 * optional second, slightly scaled quad offset above the first for depth.
 *
 * Colour behaviour:
 *  - When {@code entity.g == 1.0} ("fully charged"):
 *    the colour oscillates between CYAN (e) and MAGENTA (b) using a sine wave.
 *  - Otherwise: the colour lerps from WHITE (d) towards CYAN (e) using {@code entity.g}
 *    as the lerp factor.
 *
 * Original colour constants (gv fields: a=R, d=G, c=B, b=A):
 *   e = gv(0,255,251,255) - CYAN     (r=0,   g=255, b=251, a=255)
 *   b = gv(255,0,236,255) - MAGENTA  (r=255, g=0,   b=236, a=255)
 *   d = gv(255,255,255,0) - WHITE_0A (r=255, g=255, b=255, a=0)
 *
 * In 1.12.2:
 *   - Used Tessellator + BufferBuilder with GL11/GlStateManager manually.
 *   - Called {@code OpenGlHelper.setLightmapTextureCoords(240, 240)} for full bright.
 *   - Extended {@code Render<c4>}.
 *
 * In 1.20.1:
 *   - Extends {@code EntityRenderer<EnergyBallEntity>}.
 *   - Blending is set via {@code RenderSystem}.
 *   - Full-bright is done by passing {@code LightTexture.FULL_BRIGHT}.
 */
@OnlyIn(Dist.CLIENT)
public class EnergyBallRenderer extends EntityRenderer<EnergyBallEntity> {

    public static EnergyBallRenderer INSTANCE;

    private static final ResourceLocation TEXTURE =
        new ResourceLocation("sexmod", "textures/entity/galath/energy_ball.png");

    // Colour constants (RGBA 0-255 ints converted to 0-1 floats where needed)
    private static final RgbColor COLOR_CYAN    = new RgbColor(0,   255, 251); // e
    private static final RgbColor COLOR_MAGENTA = new RgbColor(255, 0,   236); // b
    private static final RgbColor COLOR_WHITE_A = new RgbColor(255, 255, 255); // d (alpha=0)

    private final Minecraft mc = Minecraft.getInstance();

    public EnergyBallRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        INSTANCE = this;
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(EnergyBallEntity entity) {
        return TEXTURE;
    }

    // =========================================================================
    //  Render
    // =========================================================================

    @Override
    public void render(EnergyBallEntity entity,
                       float entityYaw,
                       float partialTick,
                       PoseStack poseStack,
                       net.minecraft.client.renderer.MultiBufferSource bufferSource,
                       int packedLight) {

        LocalPlayer player = mc.player;
        if (player == null) return;

        // Compute interpolated position of entity and player
        Vec3 entityPrev = new Vec3(entity.xo, entity.yo, entity.zo);
        Vec3 entityPos  = MathUtil.lerpPosition(entityPrev, entity.position(), partialTick);
        Vec3 playerPrev = new Vec3(player.xo, player.yo, player.zo);
        Vec3 playerPos  = MathUtil.lerpPosition(playerPrev, player.position(), partialTick);

        Vec3 offset = entityPos.subtract(playerPos);

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);

        // Face camera (billboard)
        poseStack.mulPose(
            net.minecraft.client.renderer.entity.EntityRenderDispatcher.getRotation(
                mc.gameRenderer.getMainCamera(), true));

        // Scale by entity charge factor
        float g = (float) entity.getCharge();
        poseStack.scale(g, g, g);

        // Determine colours
        RgbColor col1, col2;
        if (entity.getCharge() == 1.0) {
            float time = mc.level.getGameTime() + partialTick;
            double t = 0.5 * Math.sin(time * 0.5) + 0.5;
            col1 = lerpColor(COLOR_CYAN,    COLOR_MAGENTA, t);
            col2 = lerpColor(COLOR_MAGENTA, COLOR_CYAN,    t);
        } else {
            col1 = lerpColor(COLOR_WHITE_A, COLOR_CYAN, entity.getCharge());
            col2 = col1;
        }

        // Setup render state
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buf = tesselator.getBuilder();

        // Primary quad
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        emitQuad(buf, col1, 0.0F);
        tesselator.end();

        // Secondary quad (slightly smaller, offset up)
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(0.0, 0.075, 0.0);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        emitQuad(buf, col2, 0.001F);
        tesselator.end();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    /** Emits a -0.25/+0.25 billboard quad at Z = {@code zOff}. */
    private void emitQuad(BufferBuilder buf, RgbColor col, float zOff) {
        int r = col.r(), g = col.g(), b = col.b();
        buf.vertex(-0.25, 0.0,  zOff).uv(0, 0).color(r, g, b, 255).endVertex();
        buf.vertex( 0.25, 0.0,  zOff).uv(1, 0).color(r, g, b, 255).endVertex();
        buf.vertex( 0.25, 0.5,  zOff).uv(1, 1).color(r, g, b, 255).endVertex();
        buf.vertex(-0.25, 0.5,  zOff).uv(0, 1).color(r, g, b, 255).endVertex();
    }

    /** Linearly interpolates between two {@link RgbColor} values. */
    private static RgbColor lerpColor(RgbColor a, RgbColor b, double t) {
        return new RgbColor(
            (int)(a.r() + (b.r() - a.r()) * t),
            (int)(a.g() + (b.g() - a.g()) * t),
            (int)(a.b() + (b.b() - a.b()) * t));
    }
}
