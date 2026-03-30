package com.trolmastercard.sexmod.entity.ai;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * KoboldFollowLeaderGoal — Portado a 1.20.1.
 * * Objetivo de IA que hace que los Kobolds sigan al líder de su tribu.
 * * La velocidad escala dinámicamente: entre más lejos esté del líder, más rápido correrá.
 * * Incluye un "periodo de gracia" (closeTimer) para evitar que el NPC se detenga abruptamente.
 */
public class KoboldFollowLeaderGoal extends NpcGoalBase {

    private int closeTimer = 0;

    public KoboldFollowLeaderGoal(BaseNpcEntity npc) {
        super(npc);
        // Marcamos que este objetivo ocupa los slots de MOVIMIENTO y MIRADA
        this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public void start() {
        super.start();
        // Inicializamos con una velocidad de acecho mínima
        this.d.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.02D);
    }

    @Override
    protected GoalState computeDesiredState() {
        if (this.a == null || !this.a.isAlive()) return GoalState.IDLE;

        float dist = this.d.distanceTo((LivingEntity) this.a);
        boolean tooFar = dist > 5.0F;

        // Lógica de Linger: Si no tiene pareja y está cerca, se queda un rato antes de entrar en IDLE
        if (this.d.getSexPartner() == null) {
            if (!tooFar && this.f == GoalState.FOLLOW) {
                if (++this.closeTimer > 60) { // ~3 segundos de espera
                    tooFar = false;
                    this.closeTimer = 0;
                } else {
                    tooFar = true;
                }
            }
        }

        return tooFar ? GoalState.FOLLOW : GoalState.IDLE;
    }

    @Override
    protected void onStateChanged(GoalState newState) {
        if (this.a == null) return;

        switch (newState) {
            case FOLLOW -> {
                double dist = this.d.distanceTo((LivingEntity) this.a);
                // Si el navegador está intentando ir más lejos que la posición del líder, reiniciamos
                if (this.c.getTargetDistanceSqr() > dist * dist) {
                    this.c.stop();
                }

                double speed = updateSpeed();
                this.c.moveTo((LivingEntity) this.a, speed);
            }
            case IDLE -> {
                this.c.stop();
                this.d.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.1D); // Velocidad base
            }
        }
    }

    /**
     * Calcula la velocidad basándose en la distancia.
     * Fórmula: 0.02 base + incrementos de 0.05 por cada 3 bloques, tope de 0.7.
     */
    private double updateSpeed() {
        if (this.a == null) return 0.1D;

        float dist = this.d.distanceTo((LivingEntity) this.a);
        double extra = Math.min(0.7D, Math.floor(dist / 3.0F) * 0.05D);
        double finalSpeed = 0.02D + extra;

        this.d.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(finalSpeed);
        return finalSpeed;
    }

    @Override
    public void tick() {
        super.tick();
        // Actualizamos la velocidad dinámicamente mientras lo seguimos
        if (this.f == GoalState.FOLLOW && this.d.tickCount % 10 == 0) {
            updateSpeed();
        }
    }
}