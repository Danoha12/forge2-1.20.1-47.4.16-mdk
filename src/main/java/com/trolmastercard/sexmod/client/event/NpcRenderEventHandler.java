package com.trolmastercard.sexmod.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.client.CameraController;
import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcStateAccessor;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * NpcRenderEventHandler — Portado a 1.20.1.
 * * Maneja el suavizado de cámara y renderizado especial de estados (Pick Up / Throw).
 */
@OnlyIn(Dist.CLIENT)
public class NpcRenderEventHandler {

    // =========================================================================
    //  1. Seguimiento Suave de Cámara
    // =========================================================================

    @SubscribeEvent
    public void onRenderWorldCameraSmooth(RenderLevelStageEvent event) {
        // Ejecutamos después de que las entidades se hayan procesado
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !mc.options.getCameraType().isFirstPerson()) return;

        UUID playerUUID = player.getUUID();
        BaseNpcEntity bound = null;

        // Búsqueda segura del NPC vinculado al jugador
        for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
            if (npc != null && !npc.isRemoved() && npc instanceof NpcStateAccessor state) {
                if (playerUUID.equals(state.getSexPartnerUUID())) {
                    bound = npc;
                    break;
                }
            }
        }

        if (bound == null) return;

        float partialTick = event.getPartialTick();
        CameraController cam = CameraController.getInstance(); // Usando getter estándar

        // Lógica de Inercia y Rotación
        float yaw = player.getYRot();
        cam.yawVelocity = (float)(player.input.leftImpulse * cam.mouseDelta.x);
        cam.yawVelocity += -(yaw - cam.prevYaw) * 3.0F;
        cam.smoothYawVelocity = MathUtil.lerp(cam.smoothYawVelocity, cam.yawVelocity, 0.1F);

        float pitch = -player.getXRot();
        cam.pitchVelocity = (float)(player.input.forwardImpulse * cam.mouseDelta.z + (float)player.getDeltaMovement().y * cam.mouseDelta.y);
        cam.pitchVelocity += -(pitch - cam.prevPitch) * 3.0F;
        cam.smoothPitchVelocity = MathUtil.lerp(cam.smoothPitchVelocity, cam.pitchVelocity, 0.1F);

        // El corazón de la cámara: Actualiza la posición basándose en los huesos del NPC
        cam.update(bound, partialTick);

        // Guardar estados para el siguiente frame
        cam.prevYaw = yaw;
        cam.prevPitch = pitch;

        // Limpiar estado de renderizado para evitar glitches en otros mods
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // =========================================================================
    //  2. Renderizado Forzado (Estado Throw)
    // =========================================================================

    @SubscribeEvent
    public void onRenderWorldThrow(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
            if (npc.isRemoved() || !(npc instanceof NpcStateAccessor state)) continue;

            if (npc.getAnimState() == AnimState.START_THROWING) {
                boolean isOwner = player.getUUID().equals(state.getSexPartnerUUID());

                // Forzamos el renderizado con un ángulo de rotación específico (-420.69)
                // que el renderizador de la chica usa como señal interna.
                npc.setInvisible(true); // Ocultamos la versión "normal" para que no se duplique

                mc.getEntityRenderDispatcher().render(
                        npc,
                        0.0D, 0.0D, 0.0D,
                        isOwner ? -420.69F : 0.0F,
                        event.getPartialTick(),
                        event.getPoseStack(),
                        mc.renderBuffers().bufferSource(),
                        0xF000F0 // Full Bright
                );

                npc.setInvisible(false);
                break;
            }
        }
    }

    // =========================================================================
    //  3. Cancelar Renderizado (Cuerpo y Manos)
    // =========================================================================

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        if (shouldHidePlayerContent()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (shouldHidePlayerContent(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    /** Helper para saber si debemos ocultar al jugador basado en el estado del NPC */
    private boolean shouldHidePlayerContent() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && shouldHidePlayerContent(mc.player.getUUID());
    }

    private boolean shouldHidePlayerContent(UUID playerUUID) {
        for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
            if (npc instanceof NpcStateAccessor state) {
                AnimState anim = npc.getAnimState();
                if ((anim == AnimState.PICK_UP || anim == AnimState.START_THROWING)
                        && playerUUID.equals(state.getSexPartnerUUID())) {
                    return true;
                }
            }
        }
        return false;
    }
}