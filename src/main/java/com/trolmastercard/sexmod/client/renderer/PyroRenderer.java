package com.trolmastercard.sexmod.client.renderer; // Ajusta a tu paquete correcto

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.trolmastercard.sexmod.entity.PyroEntity; // Asegúrate de importar la entidad
import com.trolmastercard.sexmod.registry.ModSounds; // Asumiendo que existe
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
 * PyroRenderer — Portado a 1.20.1.
 * * Renderiza la entidad easter-egg "Pyro" como un sprite 2D billboard (estilo Doom).
 * * La textura y animación dependen de la distancia y el estado del jugador local.
 */
@OnlyIn(Dist.CLIENT)
public class PyroRenderer extends EntityRenderer<PyroEntity> {

    // ── Texturas ─────────────────────────────────────────────────────────────

    static final ResourceLocation TEX_STANDING = new ResourceLocation("sexmod", "textures/entity/pyrocinical/standing.png");
    static final ResourceLocation TEX_PRAISING = new ResourceLocation("sexmod", "textures/entity/pyrocinical/praising.png");
    static final ResourceLocation TEX_WALK1    = new ResourceLocation("sexmod", "textures/entity/pyrocinical/walking1.png");
    static final ResourceLocation TEX_WALK2    = new ResourceLocation("sexmod", "textures/entity/pyrocinical/walking2.png");

    // La ruta base para la animación.
    static final String FAT_PREFIX = "textures/entity/pyrocinical/fat/";

    static final int FAT_FRAMES = 30;
    static final float SCALE_BASE = 1.4F;

    private long lastPraiseSoundTime = 0L;
    private ResourceLocation lastTexture = null;

    public PyroRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(PyroEntity entity) {
        // En este renderizador personalizado, la textura se selecciona dinámicamente en render()
        return TEX_STANDING;
    }

    // ── Lógica Principal de Renderizado ──────────────────────────────────────

    @Override
    public void render(PyroEntity entity, float entityYaw, float partialTick,
                       PoseStack ps, MultiBufferSource bufferSource, int packedLight) {

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer lp = mc.player;
        if (lp == null) return;

        // 1.20.1: getPosition() y getEyePosition() ya aplican LERP internamente.
        Vec3 entityPos = entity.getPosition(partialTick);
        Vec3 playerPos = lp.getPosition(partialTick);

        // Distancia Manhattan aproximada (rápida de calcular)
        Vec3 delta = entityPos.subtract(playerPos);
        double dist = Math.abs(delta.x) + Math.abs(delta.y) + Math.abs(delta.z);

        ResourceLocation tex = selectTexture(entity, dist, lp);
        float size = SCALE_BASE + getScaleAdd(entity, partialTick, lp);
        float alpha = getFadeAlpha(entity, partialTick, lp);
        double bobY = getBobY(tex, lp);

        // ── Gatillo de Sonido ──
        if (this.lastTexture != TEX_PRAISING && tex == TEX_PRAISING) {
            long now = System.currentTimeMillis();
            if (now > this.lastPraiseSoundTime + 60_000L) {
                // Asumiendo que ModSounds.MISC_PYRO[0] existe
                lp.playSound(ModSounds.MISC_PYRO[0].get(), 1.0F, 1.0F);
                this.lastPraiseSoundTime = now;
            }
        }
        this.lastTexture = tex;

        // ── Construcción del Quad (Billboard 2D) ──
        ps.pushPose();

        // El PoseStack de EntityRenderer YA ESTÁ trasladado a entityPos.
        // Solo necesitamos añadir el boteo vertical del caminar (bobY).
        ps.translate(0.0D, bobY, 0.0D);

        // Rotación Billboard: Mirar siempre a la cámara
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - this.entityRenderDispatcher.camera.getYRot()));

        // Centramos el sprite y aplicamos escala
        ps.translate(0.0D, size / 2.0D, 0.0D); // Subir la mitad para que no se hunda en el suelo
        ps.scale(size, size, size);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucentCull(tex));
        PoseStack.Pose pose = ps.last();

        int light = 15728880; // Luz máxima (FullBright) para que brille en la oscuridad

        // Vertices del plano 2D centrado (De -0.5 a 0.5)
        vc.vertex(pose.pose(), -0.5F, -0.5F, 0.0F).color(1f, 1f, 1f, alpha).uv(0f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
        vc.vertex(pose.pose(),  0.5F, -0.5F, 0.0F).color(1f, 1f, 1f, alpha).uv(1f, 1f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
        vc.vertex(pose.pose(),  0.5F,  0.5F, 0.0F).color(1f, 1f, 1f, alpha).uv(1f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();
        vc.vertex(pose.pose(), -0.5F,  0.5F, 0.0F).color(1f, 1f, 1f, alpha).uv(0f, 0f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0, 1, 0).endVertex();

        ps.popPose();

        super.render(entity, entityYaw, partialTick, ps, bufferSource, packedLight);
    }

    // ── Lógica de Animación y Estado ─────────────────────────────────────────

    private ResourceLocation selectTexture(PyroEntity entity, double dist, LocalPlayer lp) {
        if (entity.fatStartTick != -1) {
            int frame = getFatFrame(entity, lp);
            return new ResourceLocation("sexmod", FAT_PREFIX + frame + ".png");
        }
        if (dist < 3.0D) return TEX_PRAISING;

        // Si la entidad no se ha movido
        if (entity.getDeltaMovement().lengthSqr() < 0.001D) {
            return TEX_STANDING;
        }

        return (Math.sin(lp.tickCount * 0.75F) > 0.0D) ? TEX_WALK1 : TEX_WALK2;
    }

    private double getBobY(ResourceLocation tex, LocalPlayer lp) {
        if (!TEX_WALK1.equals(tex) && !TEX_WALK2.equals(tex)) return 0.0D;
        return Math.sin(lp.tickCount * 0.75F) * 0.1D;
    }

    private int getFatFrame(PyroEntity entity, LocalPlayer lp) {
        if (entity.fatStartTick == -1) return 0;
        return Mth.clamp(lp.tickCount - entity.fatStartTick, 1, FAT_FRAMES);
    }

    private float getScaleAdd(PyroEntity entity, float partialTick, LocalPlayer lp) {
        if (entity.fatStartTick == -1) return 0.0F;
        int frame = getFatFrame(entity, lp);
        if (frame >= FAT_FRAMES) return 1.0F;
        return (frame + partialTick) / FAT_FRAMES;
    }

    private float getFadeAlpha(PyroEntity entity, float partialTick, LocalPlayer lp) {
        if (entity.fatStartTick == -1) return 1.0F;
        int ticksSince = lp.tickCount - entity.fatStartTick;
        if (ticksSince > 120) return 0.0F;

        float elapsed = Mth.clamp(ticksSince, 90.0F, 120.0F) - 90.0F;
        return 1.0F - ((elapsed + partialTick) / 30.0F);
    }
}