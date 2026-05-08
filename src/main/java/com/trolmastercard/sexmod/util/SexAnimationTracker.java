package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SexAnimationTracker {

    // 🚨 ESTA ES LA PIEZA QUE FALTABA: El almacén de progreso
    private static float currentProgress = 0.0F;

    public static void setProgress(float value) {
        currentProgress = value;
    }

    // ── LÓGICA DE SERVIDOR ───────────────────────────────────────

    public static void serverTick(BaseNpcEntity npc) {
        AnimState currentState = npc.getAnimState();
        if (currentState == AnimState.NULL || currentState == null) {
            npc.setAnimTick(0);
            return;
        }

        int nextTick = npc.getAnimTick() + 1;

        if (nextTick >= currentState.length) {
            if (currentState.followUp != null) {
                npc.setAnimState(currentState.followUp);
                npc.setAnimTick(0);
            } else {
                npc.setAnimTick(currentState.length);
            }
        } else {
            npc.setAnimTick(nextTick);
        }
    }

    // ── LÓGICA DE CLIENTE ──────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    public static float getProgress() {
        // Si el progreso manual (Horny Meter) tiene algo, mandamos eso
        if (currentProgress > 0.0F) {
            return currentProgress;
        }

        // Si no, intentamos calcularlo dinámicamente como antes
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0.0F;

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof BaseNpcEntity npc) {
            return calculateNpcProgress(npc);
        }

        return BaseNpcEntity.getAllActive().stream()
                .filter(npc -> player.getUUID().equals(npc.getSexPartnerUUID()))
                .map(SexAnimationTracker::calculateNpcProgress)
                .findFirst()
                .orElse(0.0F);
    }

    @OnlyIn(Dist.CLIENT)
    private static float calculateNpcProgress(BaseNpcEntity npc) {
        AnimState state = npc.getAnimState();
        if (state == null || state.length <= 0) return 0.0F;
        return (float) npc.getAnimTick() / (float) state.length;
    }

    public static boolean canUseAllieLamp() {
        return true;
    }
}