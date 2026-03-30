package com.trolmastercard.sexmod.client.event;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.util.MathUtil;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * PlayerCamEventHandler — Portado a 1.20.1.
 * * Controla la visibilidad del jugador y la posición de la cámara durante escenas.
 */
@OnlyIn(Dist.CLIENT)
public class PlayerCamEventHandler {

    private Vec3 savedPos = null;
    private Vec3 savedOldPos = null;

    // ── 1. OCULTAR AL JUGADOR (Para que no se solape con el modelo de la chica) ──

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        UUID renderingPlayerId = event.getEntity().getUUID();

        // Iteramos sobre los NPCs activos de forma segura
        for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
            if (npc.isRemoved()) continue;

            UUID partnerId = npc.getTargetPlayerId();
            AnimState state = npc.getAnimState();

            if (partnerId != null && state != AnimState.NULL && state.hasPlayer) {
                if (partnerId.equals(renderingPlayerId)) {
                    event.setCanceled(true); // ¡Invisible!
                    return;
                }
            }
        }
    }

    // ── 2. OCULTAR MANOS (En primera persona durante escenas) ──

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) return;

        // Caso A: El jugador está siendo "poseído" por un PlayerKoboldEntity
        PlayerKoboldEntity pk = PlayerKoboldEntity.getForPlayer(localPlayer);
        if (pk != null && pk.isSexModeActive()) {
            event.setCanceled(true);
            return;
        }

        // Caso B: El jugador está en una escena con un NPC
        for (BaseNpcEntity npc : BaseNpcEntity.getAllNpcs()) {
            if (npc.isRemoved()) continue;

            UUID partnerId = npc.getTargetPlayerId();
            AnimState state = npc.getAnimState();

            if (partnerId != null && state != null && state.hasPlayer) {
                if (partnerId.equals(localPlayer.getUUID())) {
                    event.setCanceled(true);
                    return;
                }
            }
        }
    }

    // ── 3. BOY-CAM (Cámara anclada a los ojos del NPC) ──

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // FASE FINAL: Restaurar la posición real del jugador para que el motor no se pierda
        if (event.phase == TickEvent.Phase.END) {
            if (savedPos != null) {
                mc.player.setPos(savedPos.x, savedPos.y, savedPos.z);
                mc.player.xo = savedOldPos.x; // En 1.20.1 xOld es xo
                mc.player.yo = savedOldPos.y;
                mc.player.zo = savedOldPos.z;
                savedPos = null;
                savedOldPos = null;
            }
            return;
        }

        // Solo aplicamos Boy-Cam en primera persona
        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) return;

        // Buscamos si hay un NPC interactuando con nosotros que requiera Boy-Cam
        BaseNpcEntity targetNpc = BaseNpcEntity.getNpcForPlayer(mc.player.getUUID(), false);
        if (targetNpc == null) return;

        AnimState state = targetNpc.getAnimState();
        if (state == null || !state.useBoyCam) return;

        // Guardamos la posición real antes de hacer el "teletransportar" visual
        savedPos = mc.player.position();
        savedOldPos = new Vec3(mc.player.xo, mc.player.yo, mc.player.zo);

        // Obtenemos la posición del hueso "boyCam" del modelo de GeckoLib
        Vec3 boneCurrent = targetNpc.getBoneWorldPosition("boyCam");
        Vec3 bonePos;

        if (targetNpc.isSexModeActive()) {
            bonePos = boneCurrent;
        } else {
            // Interpolación para que la cámara no dé tirones si el NPC se mueve
            Vec3 boneOld = targetNpc.getLastTickBonePosition("boyCam");
            bonePos = MathUtil.lerpVec3(boneOld, boneCurrent, event.renderTickTime);
        }

        // Colocamos al jugador de modo que sus "ojos" coincidan con el hueso del NPC
        double eyeHeight = mc.player.getEyeHeight();
        mc.player.setPos(bonePos.x, bonePos.y - eyeHeight, bonePos.z);

        // Sincronizamos la posición anterior para evitar motion blur erróneo
        mc.player.xo = bonePos.x;
        mc.player.yo = bonePos.y - eyeHeight;
        mc.player.zo = bonePos.z;
    }
}