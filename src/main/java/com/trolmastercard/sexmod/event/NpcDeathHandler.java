package com.trolmastercard.sexmod.event;
import com.trolmastercard.sexmod.BaseNpcEntity;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * NpcDeathHandler - ported from eo.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 * Removes a dying BaseNpcEntity from the global tracking list.
 *
 * 1.12.2 - 1.20.1:
 *   - net.minecraftforge.fml.common.eventhandler - net.minecraftforge.eventbus.api
 *   - em.ad() - BaseNpcEntity.getAllNpcs()
 */
public class NpcDeathHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof BaseNpcEntity npc) {
            BaseNpcEntity.getAllNpcs().remove(npc);
        }
    }
}
