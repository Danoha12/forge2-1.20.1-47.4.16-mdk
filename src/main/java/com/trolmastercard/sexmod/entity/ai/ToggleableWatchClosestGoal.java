package com.trolmastercard.sexmod.entity.ai; // Ajusta a tu paquete de IA

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;

/**
 * ToggleableWatchClosestGoal — Portado a 1.20.1.
 * * Subclase de LookAtPlayerGoal con un interruptor de encendido/apagado.
 * * 🚨 CORREGIDO: Interviene en la validación de la meta en lugar de solo en el tick
 * * para evitar que los NPCs se queden con la cabeza congelada.
 */
public class ToggleableWatchClosestGoal extends LookAtPlayerGoal {

    public boolean enabled = true;

    public ToggleableWatchClosestGoal(Mob mob, Class<? extends LivingEntity> watchClass, float range, float probability) {
        super(mob, watchClass, range, probability);
    }

    // ── Ciclo de Vida de la Meta (Goal) ──────────────────────────────────────

    @Override
    public boolean canUse() {
        // Solo podemos empezar a mirar si el interruptor está encendido
        return this.enabled && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Si apagan el interruptor a mitad de camino, forzamos a que la meta se detenga
        return this.enabled && super.canContinueToUse();
    }

    // Ya no necesitamos sobrescribir tick(). Si canContinueToUse devuelve false,
    // Minecraft dejará de llamar a tick() automáticamente.
}