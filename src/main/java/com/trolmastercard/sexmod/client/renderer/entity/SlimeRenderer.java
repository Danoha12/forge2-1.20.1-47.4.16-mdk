package com.trolmastercard.sexmod.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.client.handler.KoboldShoulderRenderHandler;
import com.trolmastercard.sexmod.client.model.entity.SlimeModel;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.entity.SlimeEntity;
import com.trolmastercard.sexmod.util.NpcColorData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * SlimeRenderer — Reconstruido para 1.20.1 / GeckoLib 4.
 * * Renderizador principal de la Slime.
 * * Configurado para soportar capas translúcidas (Translucent RenderType)
 * para que el cuerpo de slime sea semitransparente.
 */
@OnlyIn(Dist.CLIENT)
public class SlimeRenderer extends NpcBodyRenderer<SlimeEntity> {

    public SlimeRenderer(EntityRendererProvider.Context ctx) {
        // Enlazamos el renderizador con el modelo que acabamos de portar
        super(ctx, new SlimeModel());
    }

    // ── Transparencia (VITAL para la Slime) ───────────────────────────────────

    @Override
    public RenderType getRenderType(SlimeEntity animatable, ResourceLocation texture,
                                    MultiBufferSource bufferSource, float partialTick) {
        // En GeckoLib 4, esto obliga al motor a procesar los canales Alpha (transparencia) de tu textura.
        // Si no pones esto, la slime se verá como un bloque sólido.
        return RenderType.entityTranslucent(texture);
    }

    // ── Lógica de Renderizado Principal ───────────────────────────────────────

    @Override
    public void render(SlimeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        // Escalar la Slime si es necesario (En tu PlayerKobold vi que usas Scale 1.6F)
        // Puedes ajustar este scale si la Slime base también debe ser más grande.
        poseStack.pushPose();
        poseStack.scale(1.0F, 1.0F, 1.0F);

        // Alineación de rotación durante escenas de interacción
        if (entity.isSexModeActive()) {
            java.util.UUID partnerId = entity.getPartnerUUID();
            if (partnerId != null) {
                Player partner = entity.level().getPlayerByUUID(partnerId);
                if (partner != null) {
                    entity.yHeadRot = partner.getYRot();
                    entity.yBodyRot = partner.getYRot();
                    entityYaw = partner.getYRot();
                }
            }
        }

        // Llamada al renderizador base (dibuja la malla)
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        // Sombras y contornos (Outline Shader)
        NpcColorData.applyOutlineTinting(entity, partialTick);

        poseStack.popPose();
    }

    // ── Helper para el sistema de Avatares (Shoulder Render / Player Kobold) ──

    /**
     * Renders a SlimeEntity as if it were at the given player's position.
     * Requerido para que el jugador se vea como la Slime durante las escenas en primera/tercera persona.
     */
    public static void renderForPlayer(SlimeEntity entity, Player player,
                                       double dx, double dy, double dz, float pt) {
        // Reutilizamos el puente mágico que hicimos en KoboldShoulderRenderHandler
        KoboldShoulderRenderHandler.renderAsPlayer((PlayerKoboldEntity) (Object) entity, player,
                new PoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(),
                15728880, pt);
    }
}