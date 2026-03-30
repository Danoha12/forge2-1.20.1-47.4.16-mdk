package com.trolmastercard.sexmod.entity.ai.goal;

import com.trolmastercard.sexmod.entity.KoboldEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

import java.util.function.Predicate;

/**
 * TribeAttackGoal — Portado a 1.20.1.
 * * IA que permite a los monstruos detectar y atacar a los Kobolds de las tribus.
 * * Integrado nativamente con el motor de TargetingConditions de Minecraft.
 */
public class TribeAttackGoal extends NearestAttackableTargetGoal<KoboldEntity> {

    private final boolean healthCheck;

    public TribeAttackGoal(Mob attacker) {
        this(attacker, true, false);
    }

    public TribeAttackGoal(Mob attacker, boolean mustSee, boolean healthCheck) {
        // Pasamos el filtro (Predicado) directamente al constructor padre.
        super(attacker, KoboldEntity.class, 10, mustSee, false, (Predicate<LivingEntity>) entity -> {
            if (entity instanceof KoboldEntity kobold) {
                // Filtro: Ignora a los Kobolds si están muertos, borrados o "ocupados" en cinemáticas
                return !kobold.isRemoved() && kobold.isAlive() && !kobold.isInteractiveModeActive();
            }
            return false;
        });

        this.healthCheck = healthCheck;
    }

    // ── Lógica de Interrupción Custom ────────────────────────────────────────

    @Override
    public boolean canUse() {
        // 1. Verificación de salud (Health-gate de autopreservación)
        if (this.healthCheck && this.mob.getHealth() < this.mob.getMaxHealth() * 0.5F) {
            return false;
        }

        // 2. Dejamos que la clase padre maneje el escaneo pesado y los ticks
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Si el monstruo recibe mucho daño durante el combate, se rinde y deja al Kobold en paz
        if (this.healthCheck && this.mob.getHealth() < this.mob.getMaxHealth() * 0.5F) {
            return false;
        }

        return super.canContinueToUse();
    }
}