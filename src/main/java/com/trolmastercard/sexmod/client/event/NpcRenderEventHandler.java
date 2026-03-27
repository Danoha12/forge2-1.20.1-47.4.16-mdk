package com.trolmastercard.sexmod.client.event;

import com.mojang.blaze3d.systems.RenderSystem;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcStateAccessor;
import com.trolmastercard.sexmod.util.MathUtil; // Asegúrate de que esta ruta sea correcta

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ConcurrentModificationException;
import java.util.UUID;

/**
 * NpcRenderEventHandler - ported to 1.20.1.
 */
@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class NpcRenderEventHandler {

    // Variables extraídas para el suavizado de cámara (ya que Vanilla Camera no las tiene)
    private static float yawVelocity = 0;
    private static float smoothYawVelocity = 0;
    private static float prevYaw = 0;

    private static float pitchVelocity = 0;
    private static float smoothPitchVelocity = 0;
    private static float prevPitch = 0;

    // =========================================================================
    //  1. Camera smooth tracking
    // =========================================================================

    @SubscribeEvent
    public static void onRenderWorldCameraSmooth(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Solo activo en primera persona
        if (mc.options.getCameraType().ordinal() != 0) return;

        UUID playerUUID = player.getUUID();
        BaseNpcEntity bound = null;

        try {
            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (npc == null || npc.isRemoved() || !npc.level().isClientSide()) continue;
                if (!(npc instanceof NpcStateAccessor state)) continue;
                if (playerUUID.equals(state.getSexPartnerUUID())) {
                    bound = npc;
                    break;
                }
            }
        } catch (ConcurrentModificationException ignored) {}

        if (bound == null) return;

        float partialTick = event.getPartialTick();

        float yaw = player.getYRot();
        // Usamos variables locales estáticas en lugar de intentar inyectarlas en la cámara de Vanilla
        yawVelocity = (float)(player.input.leftImpulse * 0.5f); // Simplificado porque mouseDelta está oculto en 1.20
        yawVelocity += -(yaw - prevYaw) * 3.0F;
        yawVelocity = MathUtil.lerp(smoothYawVelocity, yawVelocity, 0.1F);

        float pitch = -player.getXRot();
        pitchVelocity = (float)(player.input.forwardImpulse * 0.5f + player.getDeltaMovement().y * 0.5f);
        pitchVelocity += -(pitch - prevPitch) * 3.0F;
        pitchVelocity = MathUtil.lerp(smoothPitchVelocity, pitchVelocity, 0.1F);

        // TODO: Si tenías un método "cam.update(bound, partialTick);", tendrás que
        // aplicar la lógica de movimiento directamente a player.setYRot() / setXRot() aquí.

        prevYaw = yaw;
        smoothYawVelocity = yawVelocity;
        prevPitch = pitch;
        smoothPitchVelocity = pitchVelocity;

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.enableCull();
    }

    // =========================================================================
    //  2. Re-render NPC in throw state
    // =========================================================================

    @SubscribeEvent
    public static void onRenderWorldThrow(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        UUID playerUUID = player.getUUID();

        try {
            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (!npc.level().isClientSide() || npc.isRemoved()) continue;
                if (!(npc instanceof NpcStateAccessor state)) continue;

                if (npc.getAnimState() != AnimState.START_THROWING) continue;

                boolean isOwner = playerUUID.equals(state.getSexPartnerUUID());

                // Temporarily enable invisible so vanilla skips it
                npc.setInvisible(true);
                mc.getEntityRenderDispatcher().render(
                        npc,
                        0.0, 0.0, 0.0,
                        isOwner ? -420.69F : 0.0F,
                        event.getPartialTick(), // Reemplaza mc.getFrameTime()
                        event.getPoseStack(),
                        mc.renderBuffers().bufferSource(),
                        0xF000F0 // Luz máxima
                );
                npc.setInvisible(false);

                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.enableCull();
                return;
            }
        } catch (ConcurrentModificationException ignored) {}
    }

    // =========================================================================
    //  3. Cancel hand render
    // =========================================================================

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        UUID playerUUID = player.getUUID();

        try {
            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (!(npc instanceof NpcStateAccessor state)) continue;

                AnimState anim = npc.getAnimState();
                if (anim != AnimState.PICK_UP && anim != AnimState.START_THROWING) continue;

                if (playerUUID.equals(state.getSexPartnerUUID())) {
                    event.setCanceled(true);
                    return;
                }
            }
        } catch (ConcurrentModificationException ignored) {}
    }

    // =========================================================================
    //  4. Cancel player body render
    // =========================================================================

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        UUID playerUUID = event.getEntity().getUUID();

        try {
            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (!(npc instanceof NpcStateAccessor state)) continue;

                AnimState anim = npc.getAnimState();
                if (anim != AnimState.PICK_UP && anim != AnimState.START_THROWING) continue;

                if (playerUUID.equals(state.getSexPartnerUUID())) {
                    event.setCanceled(true);
                    return;
                }
            }
        } catch (ConcurrentModificationException ignored) {}
    }
}