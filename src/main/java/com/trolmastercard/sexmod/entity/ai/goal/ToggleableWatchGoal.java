package com.trolmastercard.sexmod.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

/**
 * ToggleableWatchGoal — Portado a 1.20.1.
 * * Envuelve LookAtPlayerGoal con un interruptor booleano.
 * * Permite pausar el seguimiento visual de la cabeza sin remover el Goal de la entidad.
 */
public class ToggleableWatchGoal extends LookAtPlayerGoal {

    /** Si es false, el NPC dejará de mover la cabeza para seguir al objetivo. */
    public boolean enabled = true;

    public ToggleableWatchGoal(Mob mob,
                               Class<? extends LivingEntity> watchTarget,
                               float maxDistance,
                               float chance) {
        // En 1.20.1, el segundo parámetro suele ser LivingEntity.class o Player.class
        super(mob, watchTarget, maxDistance, chance);
    }

    @Override
    public boolean canUse() {
        // Si el Goal está desactivado, ni siquiera intentamos buscar un objetivo
        return enabled && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Si se desactiva mientras está en uso, dejamos de seguir
        return enabled && super.canContinueToUse();
    }

    @Override
    public void tick() {
        // Doble guardia por seguridad
        if (enabled) {
            super.tick();
        }
    }
}