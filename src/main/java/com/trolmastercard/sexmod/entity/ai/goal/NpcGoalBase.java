package com.trolmastercard.sexmod.entity.ai.goal;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

import java.util.EnumSet;
import java.util.UUID;

/**
 * NpcGoalBase — Portado a 1.20.1.
 * * Clase base abstracta para todas las metas (Goals) de los NPCs.
 * * Gestiona la relación Dueño-NPC, velocidades y estados.
 */
public abstract class NpcGoalBase extends Goal {

    public static final double SPEED_WALK = 0.5D;
    public static final double SPEED_RUN  = 0.7D;

    protected final BaseNpcEntity npc;
    protected Player owner;
    protected PathNavigation navigation;
    protected State currentState = State.IDLE;

    public NpcGoalBase(BaseNpcEntity npc) {
        this.npc = npc;
        this.navigation = npc.getNavigation();
        // Definimos que este Goal usa el movimiento y la mirada
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    // ── Ciclo de Vida del Goal ───────────────────────────────────────────────

    @Override
    public boolean canUse() {
        // Verificamos si el NPC tiene un dueño asignado
        String ownerUUID = npc.getEntityData().get(BaseNpcEntity.DATA_OWNER_UUID);
        if (ownerUUID.isEmpty()) return false;

        this.owner = npc.level().getPlayerByUUID(UUID.fromString(ownerUUID));
        return this.owner != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (this.owner == null || !this.owner.isAlive()) return false;

        String ownerUUID = npc.getEntityData().get(BaseNpcEntity.DATA_OWNER_UUID);
        return !ownerUUID.isEmpty();
    }

    @Override
    public void start() {
        // Se ejecuta una vez cuando el Goal se activa
        this.onActivate();
    }

    public void onActivate() {
        this.navigation = npc.getNavigation();
    }

    @Override
    public void stop() {
        // Limpieza al desactivar el Goal
        if (this.navigation != null) {
            this.navigation.stop();
        }
        this.currentState = State.IDLE;
        this.owner = null;
    }

    @Override
    public void tick() {
        if (this.owner == null) return;

        // Actualizamos el estado lógico
        this.currentState = computeNextState();

        // Aplicamos la lógica del estado (movimiento, etc.)
        handleState(this.currentState);
    }

    // ── Métodos Abstractos ───────────────────────────────────────────────────

    protected abstract State computeNextState();

    protected abstract void handleState(State state);

    // ── Utilidades para Subclases ────────────────────────────────────────────

    /**
     * Teletransporta al NPC cerca del jugador si se queda atascado.
     */
    protected void teleportNearOwner() {
        if (this.owner == null) return;

        BlockPos ownerPos = this.owner.blockPosition();
        for (int i = 0; i < 10; ++i) {
            int x = npc.getRandom().nextInt(7) - 3;
            int y = npc.getRandom().nextInt(3) - 1;
            int z = npc.getRandom().nextInt(7) - 3;

            BlockPos targetPos = ownerPos.offset(x, y, z);
            if (npc.level().getBlockState(targetPos).isAir() && npc.level().getBlockState(targetPos.below()).isSolid()) {
                npc.teleportTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D);
                this.navigation.stop();
                return;
            }
        }
    }

    /**
     * Ajusta la velocidad del NPC según si el dueño corre o camina.
     */
    protected void updateSpeedByOwner() {
        if (this.owner == null) return;

        double speed = this.owner.isSprinting() ? SPEED_RUN : SPEED_WALK;
        float distance = npc.distanceTo(this.owner);

        // Aumentar velocidad si el dueño está muy lejos
        if (distance > 10.0F) {
            speed += 0.2D;
        }

        this.navigation.setSpeedModifier(speed);

        // Actualizar animación según velocidad
        if (speed > SPEED_WALK) {
            npc.setAnimState(AnimState.RUN);
        } else if (distance > 1.5F) {
            npc.setAnimState(AnimState.WALK);
        } else {
            npc.setAnimState(AnimState.NULL);
        }
    }

    // ── Enumeración de Estados ───────────────────────────────────────────────

    public enum State {
        ATTACK,
        FOLLOW,
        IDLE,
        RIDE,
        DOWNED
    }
}