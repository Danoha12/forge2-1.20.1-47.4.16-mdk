package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;

/**
 * PyroRenderer - ported from e.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Renders the "Pyrocinical" easter-egg entity as a billboard sprite quad.
 * The texture is selected based on distance from the local player and
 * whether the entity is in a "fat" state or a walking animation.
 *
 * Textures:
 *   standing  - sexmod:textures/entity/pyrocinical/standing.png
 *   praising  - sexmod:textures/entity/pyrocinical/praising.png
 *   walking1  - sexmod:textures/entity/pyrocinical/walking1.png
 *   walking2  - sexmod:textures/entity/pyrocinical/walking2.png
 *   fat/N     - sexmod:textures/entity/pyrocinical/fat/N.png  (frames 1-30)
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - Render&lt;al&gt; - EntityRenderer&lt;PyroEntity&gt;
 *   - BufferBuilder + Tessellator - VertexConsumer from MultiBufferSource
 *   - DefaultVertexFormats.field_181707_g - DefaultVertexFormat.POSITION_TEX
 *   - GlStateManager.func_179094_E/F - poseStack.pushPose/popPose
 *   - GlStateManager.func_179137_b - poseStack.translate
 *   - GlStateManager.func_179114_b - poseStack.mulPose(Axis.YP.rotationDegrees)
 *   - GlStateManager.func_179152_a - poseStack.scale
 *   - GlStateManager.func_179131_c - vertex color (set per-vertex)
 *   - OpenGlHelper.func_77475_a (lightmap override) - use max brightness (15728880)
 *   - GL11.glDisable(2896) - lighting handled by shader
 *   - entityPlayerSP.field_70173_aa - localPlayer.tickCount
 *   - be.b(val,min,max) - Mth.clamp
 *   - b6.a(prev, cur, t) - MathUtil.lerpVec3
 *   - al.a - PyroEntity.fatStartTick; entity field directly
 *   - c.MISC_PYRO[0] - ModSounds.MISC_PYRO[0]
 */
@OnlyIn(Dist.CLIENT)
public class PyroRenderer extends EntityRenderer<PyroEntity> {

    // -- Textures ---------------------------------------------------------------
    static final ResourceLocation TEX_STANDING =
            new ResourceLocation("sexmod", "textures/entity/pyrocinical/standing.png");
    static final ResourceLocation TEX_PRAISING =
            new ResourceLocation("sexmod", "textures/entity/pyrocinical/praising.png");
    static final ResourceLocation TEX_WALK1    =
            new ResourceLocation("sexmod", "textures/entity/pyrocinical/walking1.png");
    static final ResourceLocation TEX_WALK2    =
            new ResourceLocation("sexmod", "textures/entity/pyrocinical/walking2.png");
    static final String           FAT_PREFIX   = "textures/entity/pyrocinical/fat/";

    static final int   FAT_FRAMES  = 30;
    static final float SCALE_BASE  = 1.4F;
    static final float WALK_BOB_Y  = 0.75F;

    // Cooldown between praising sounds
    long lastPraiseSoundTime = 0L;

    private ResourceLocation lastTexture = null;

    public PyroRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    // -- EntityRenderer contract ------------------------------------------------

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(PyroEntity entity) {
        return null; // texture selected dynamically
    }

    @Override
    public void render(PyroEntity entity, float entityYaw, float partialTick,
                       PoseStack ps, MultiBufferSource bufferSource, int packedLight) {

        Minecraft mc   = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        if (lp == null) return;

        Vec3 entityPos = MathUtil.lerpVec3(
                new Vec3(entity.xOld, entity.yOld, entity.zOld),
                entity.position(), partialTick);
        Vec3 playerPos = MathUtil.lerpVec3(
                new Vec3(lp.xOld, lp.yOld, lp.zOld),
                lp.position(), partialTick);

        Vec3  delta    = entityPos.subtract(playerPos);
        double dist    = Math.abs(delta.x) + Math.abs(delta.y) + Math.abs(delta.z);

        ResourceLocation tex  = selectTexture(entity, dist);
        float            size = SCALE_BASE + getScaleAdd(entity, partialTick);
        float            alpha = getFadeAlpha(entity, partialTick);
        double           bobY  = getBobY(tex);

        // --- Sound trigger ---
        if (lastTexture != TEX_PRAISING && tex == TEX_PRAISING) {
            long now = System.currentTimeMillis();
            if (now > lastPraiseSoundTime + 60_000L) {
                lp.playSound(ModSounds.MISC_PYRO[0], 1.0F, 1.0F);
                lastPraiseSoundTime = now;
            }
        }
        lastTexture = tex;

        // --- Render billboard quad ---
        ps.pushPose();
        ps.translate(delta.x, delta.y + bobY, delta.z);
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - this.entityRenderDispatcher.camera.getYRot()));
        ps.scale(size, size, size);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(tex));
        PoseStack.Pose pose = ps.last();

        int light = 15728880; // max brightness for billboards

        vc.vertex(pose.pose(), -1.0F, 0.0F, 0.0F).color(1f, 1f, 1f, alpha).uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
        vc.vertex(pose.pose(),  1.0F, 0.0F, 0.0F).color(1f, 1f, 1f, alpha).uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
        vc.vertex(pose.pose(),  1.0F, 2.0F, 0.0F).color(1f, 1f, 1f, alpha).uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
        vc.vertex(pose.pose(), -1.0F, 2.0F, 0.0F).color(1f, 1f, 1f, alpha).uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();

        ps.popPose();
    }

    // -- Helpers ----------------------------------------------------------------

    ResourceLocation selectTexture(PyroEntity entity, double dist) {
        if (entity.fatStartTick != -1) {
            int frame = getFatFrame(entity);
            return new ResourceLocation("sexmod",
                    String.format("%s%d.png", FAT_PREFIX, frame));
        }
        if (dist < 3.0D) return TEX_PRAISING;

        // If entity hasn't moved - standing
        Vec3 delta = new Vec3(entity.xOld - entity.getX(),
                               entity.yOld - entity.getY(),
                               entity.zOld - entity.getZ());
        double moved = Math.abs(delta.x) + Math.abs(delta.y) + Math.abs(delta.z);
        if (moved == 0.0D) return TEX_STANDING;

        LocalPlayer lp = Minecraft.getInstance().player;
        if (lp == null) return TEX_STANDING;
        return (Math.sin(lp.tickCount * 0.75F) > 0.0D) ? TEX_WALK1 : TEX_WALK2;
    }

    double getBobY(ResourceLocation tex) {
        if (!TEX_WALK1.equals(tex) && !TEX_WALK2.equals(tex)) return 0.0D;
        LocalPlayer lp = Minecraft.getInstance().player;
        if (lp == null) return 0.0D;
        return Math.sin(lp.tickCount * 0.75F) * 0.1D;
    }

    int getFatFrame(PyroEntity entity) {
        LocalPlayer lp = Minecraft.getInstance().player;
        if (entity.fatStartTick == -1 || lp == null) return 0;
        return (int) Mth.clamp(lp.tickCount - entity.fatStartTick, 1.0F, FAT_FRAMES);
    }

    float getScaleAdd(PyroEntity entity, float partialTick) {
        if (entity.fatStartTick == -1) return 0.0F;
        int frame = getFatFrame(entity);
        if (frame == FAT_FRAMES) return 1.0F;
        LocalPlayer lp = Minecraft.getInstance().player;
        if (lp == null) return 0.0F;
        return (frame + partialTick) / FAT_FRAMES;
    }

    float getFadeAlpha(PyroEntity entity, float partialTick) {
        if (entity.fatStartTick == -1) return 1.0F;
        LocalPlayer lp = Minecraft.getInstance().player;
        if (lp == null) return 1.0F;
        if (lp.tickCount - entity.fatStartTick > 120) return 0.0F;
        float elapsed = Mth.clamp(lp.tickCount - entity.fatStartTick, 90.0F, 120.0F) - 90.0F;
        return 1.0F - (elapsed + partialTick) / 30.0F;
    }
}
