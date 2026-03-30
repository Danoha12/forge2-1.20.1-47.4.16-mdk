package com.trolmastercard.sexmod.entity.ai;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;

import java.util.EnumSet;
import java.util.List;

/**
 * NpcBreedGoal — Portado a 1.20.1.
 * * Objetivo de IA: El aldeano busca una pareja cercana, se aproxima
 * * y genera un descendiente después de un tiempo de espera.
 */
public class NpcBreedGoal extends Goal {

    private final Villager villager;
    private Villager partner;
    private final Level level;
    private int timer;

    public NpcBreedGoal(Villager villager) {
        this.villager = villager;
        this.level = villager.level();
        // Flag.MOVE: Permite caminar. Flag.LOOK: Permite girar la cabeza.
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Solo buscamos pareja si no estamos en medio de un proceso
        if (this.timer != 0 || this.villager.getAge() != 0) return false;

        // Buscamos aldeanos en un radio de 8x3x8
        List<Villager> nearby = this.level.getEntitiesOfClass(
                Villager.class,
                this.villager.getBoundingBox().inflate(8, 3, 8),
                e -> e != this.villager && e.getAge() == 0 // Solo adultos listos
        );

        if (nearby.isEmpty()) return false;

        this.partner = nearby.get(0);
        return this.partner != null && this.partner.isAlive();
    }

    @Override
    public void start() {
        // 300 ticks = 15 segundos de "cortejo"
        this.timer = 300;
        this.villager.setInLove(null); // Activa el estado visual de amor
    }

    @Override
    public void stop() {
        this.partner = null;
        this.timer = 0;
    }

    @Override
    public boolean canContinueToUse() {
        // Continuar mientras la pareja viva y el cronómetro no haya terminado
        return this.partner != null && this.partner.isAlive() && this.timer > 0;
    }

    @Override
    public void tick() {
        this.timer--;

        // Mirar a la pareja
        this.villager.getLookControl().setLookAt(this.partner, 10.0F, 30.0F);

        // Si estamos lejos (más de 2.25 bloques cuadrados), caminamos hacia ella
        if (this.villager.distanceToSqr(this.partner) > 2.25D) {
            this.villager.getNavigation().moveTo(this.partner, 0.5D); // Velocidad normal de aldeano
        } else {
            // Si estamos cerca, nos quedamos quietos y esperamos el clímax
            this.villager.getNavigation().stop();
        }

        // Efecto visual de corazones aleatorios (EntityEvent 12 son los corazones)
        if (this.villager.getRandom().nextInt(20) == 0) {
            this.level.broadcastEntityEvent(this.villager, EntityEvent.IN_LOVE_HEARTS);
        }

        if (this.timer <= 0) {
            this.breed();
        }
    }

    private void breed() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;

        // Creamos al bebé usando la lógica nativa del aldeano
        AgeableMob baby = this.villager.getBreedOffspring(serverLevel, this.partner);

        // Ponemos a los padres en "cooldown" (6000 ticks = 5 minutos)
        this.villager.setAge(6000);
        this.partner.setAge(6000);
        this.villager.resetLove();
        this.partner.resetLove();

        // Disparamos el evento de Forge para que otros mods puedan interactuar
        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(this.villager, this.partner, baby);
        boolean cancelled = MinecraftForge.EVENT_BUS.post(event);

        if (!cancelled && event.getChild() != null) {
            AgeableMob child = event.getChild();
            child.setBaby(true);
            child.moveTo(this.villager.getX(), this.villager.getY(), this.villager.getZ(), 0.0F, 0.0F);
            serverLevel.addFreshEntityWithPassengers(child);

            // Explosión de partículas finales
            serverLevel.broadcastEntityEvent(child, EntityEvent.IN_LOVE_HEARTS);
        }

        // Auto-eliminación del objetivo para no repetir en bucle infinito
        this.villager.goalSelector.removeGoal(this);
    }
}