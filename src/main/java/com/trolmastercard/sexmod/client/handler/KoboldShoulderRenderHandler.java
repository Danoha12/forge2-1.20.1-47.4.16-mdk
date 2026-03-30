package com.trolmastercard.sexmod.client.handler;

import com.mojang.blaze3d.vertex.PoseStack;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.client.renderer.entity.NpcBodyRenderer;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector2f;

import java.util.UUID;

/**
 * KoboldShoulderRenderHandler — Portado a 1.20.1.
 * * Intercepta el renderizado del jugador para superponer el modelo de Kobold.
 * * Maneja la sincronización de movimientos, rotaciones y el efecto de "bobbing" (balanceo).
 */
@OnlyIn(Dist.CLIENT)
public class KoboldShoulderRenderHandler {

    /** Valor centinela para evitar recursión infinita en el renderizado. */
    public static final float SENTINEL = 1.2345679F;

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // Si el tick parcial es nuestro centinela, es que nosotros mismos lanzamos este render
        if (event.getPartialTick() == SENTINEL) return;

        Player player = event.getEntity();
        UUID playerId = player.getGameProfile().getId();

        // Limpieza de referencias muertas
        PlayerKoboldEntity.cleanup();

        PlayerKoboldEntity kobold = PlayerKoboldEntity.getForPlayer(playerId);
        if (kobold == null) return;

        // Cancelamos el renderizado del jugador humano
        event.setCanceled(true);

        // Dibujamos al Kobold en su lugar
        renderAsPlayer(kobold, player, event.getPoseStack(), event.getMultiBufferSource(),
                event.getPackedLight(), event.getPartialTick());
    }

    /**
     * Mapea el estado del jugador a la entidad Kobold y la renderiza.
     */
    public static void renderAsPlayer(PlayerKoboldEntity kobold, Player player, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        // Espejo de estado: Copiamos rotaciones y posiciones
        kobold.setYRot(player.getYRot());
        kobold.yRotO = player.yRotO;
        kobold.yHeadRot = player.yHeadRot;
        kobold.yHeadRotO = player.yHeadRotO;
        kobold.yBodyRot = player.yBodyRot;
        kobold.yBodyRotO = player.yBodyRotO;
        kobold.setXRot(player.getXRot());
        kobold.xRotO = player.xRotO;

        // Sincronización de físicas y flags
        kobold.setOnGround(player.isOnGround());
        kobold.setSprinting(player.isSprinting());
        kobold.setShiftKeyDown(player.isShiftKeyDown());
        kobold.isFallFlying = player.isFallFlying();

        // Sincronización de animaciones de caminata (1.20.1 usa walkAnimation)
        kobold.walkAnimation.setSpeed(player.walkAnimation.speed());
        kobold.walkAnimation.position(player.walkAnimation.position());

        // Cálculo del FootOffset (para animaciones de pies/suelo)
        double yawRad = Math.toRadians(player.getYRot());
        double dx = player.xo - player.getX();
        double dz = player.zo - player.getZ();
        kobold.footOffset = new Vector2f(
                (float)(dx * Math.cos(yawRad) + dz * Math.sin(yawRad)),
                (float)(dx * Math.sin(yawRad) + dz * Math.cos(yawRad))
        );

        // Aplicar offset vertical si está en modo "hombro"
        float yOffset = kobold.isShouldRideOffset() ? computeBobbingOffset(player, partialTick) : 0.0F;

        // Renderizado final
        NpcBodyRenderer.RENDERING_SHOULDER = true;
        try {
            poseStack.pushPose();
            poseStack.translate(0, yOffset, 0);

            dispatcher.render(kobold, 0, 0, 0, player.getYRot(), SENTINEL,
                    poseStack, bufferSource, packedLight);

            poseStack.popPose();
        } finally {
            NpcBodyRenderer.RENDERING_SHOULDER = false;
        }
    }

    /**
     * Calcula el balanceo vertical para que el Kobold no parezca una estatua al caminar.
     */
    private static float computeBobbingOffset(Player player, float partialTick) {
        // Si hay una escena activa, desactivamos el bobbing para no romper la cámara
        // if (player.getEntityData().get(BaseNpcEntity.IS_SEX_ACTIVE)) return 0.0F;

        float speed = player.walkAnimation.speed(partialTick);
        float pos = player.walkAnimation.position(partialTick);

        // Fórmula de seno para el balanceo rítmico
        return (float) (Math.sin(pos * 0.6662F) * 0.05F * speed);
    }
}