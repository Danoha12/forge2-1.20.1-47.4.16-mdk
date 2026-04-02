package com.trolmastercard.sexmod.util;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;

public class SexAnimationTracker {

    /**
     * Procesa el avance de la animación en el SERVIDOR.
     */
    public static void serverTick(BaseNpcEntity npc) {
        AnimState currentState = npc.getAnimState();
        if (currentState == AnimState.NULL) {
            npc.setAnimTick(0);
            return;
        }

        // 1. Avanzar el tiempo
        int nextTick = npc.getAnimTick() + 1;

        // 2. ¿Ha terminado la animación actual?
        if (nextTick >= currentState.length) {
            if (currentState.followUp != null) {
                // Transición automática al siguiente estado (ej: loop o terminar)
                npc.setAnimState(currentState.followUp);
                npc.setAnimTick(0);
            } else {
                // Si no hay followUp, simplemente dejamos de contar
                npc.setAnimTick(currentState.length);
            }
        } else {
            npc.setAnimTick(nextTick);
        }
    }
}