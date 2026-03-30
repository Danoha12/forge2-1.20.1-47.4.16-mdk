package com.trolmastercard.sexmod.event;

import com.trolmastercard.sexmod.data.GalathOwnershipData;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NpcDeathHandler — Portado a 1.20.1.
 * * Maneja la limpieza cuando un NPC muere.
 */
public class NpcDeathHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingDeath(LivingDeathEvent event) {
        // Verificamos si la entidad que murió es uno de nuestros NPCs
        if (event.getEntity() instanceof BaseNpcEntity npc) {

            // 1. Limpieza de la lista global de rastreo
            BaseNpcEntity.getAllNpcs().remove(npc);

            // 2. Limpieza proactiva de propiedad (Ownership)
            // Si la chica que murió tenía dueño, avisamos al sistema para que
            // el jugador recupere su libertad y se limpien los datos NBT.
            if (!npc.level().isClientSide()) {
                GalathOwnershipData.removeOwnership(npc);
            }

            // 3. Reset de estados (Opcional por seguridad)
            // Esto asegura que si el NPC estaba en una escena, los flags se limpien.
            npc.setInteractiveMode(false);
        }
    }
}