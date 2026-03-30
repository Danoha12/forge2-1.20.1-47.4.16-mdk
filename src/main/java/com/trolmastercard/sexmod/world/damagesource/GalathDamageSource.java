package com.trolmastercard.sexmod.world.damagesource;

import com.trolmastercard.sexmod.entity.GalathEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

/**
 * GalathDamageSource — Portado a 1.20.1.
 * * Fuente de daño especial para el drenaje de Galath.
 * * Ignora armadura y no aplica empuje (knockback).
 * * Mensaje de muerte personalizado.
 */
public class GalathDamageSource extends DamageSource {

    private final GalathEntity galath;

    public GalathDamageSource(GalathEntity galath) {
        // En 1.20.1 obtenemos el tipo de daño MOB_ATTACK como base para la lógica de IA.
        // Pasamos a Galath como atacante directo y causante.
        super(galath.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.MOB_ATTACK),
                galath, galath, galath.position());
        this.galath = galath;
    }

    /**
     * Mensaje de muerte: "{nombre} got his cum drained by a Succubus".
     */
    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String victimName = victim.getName().getString();
        return Component.literal(victimName + " got his cum drained by a Succubus");
    }

    /**
     * Tratamos el daño como "proyectil" para anular el knockback vanilla
     * y que el jugador no salga volando durante la escena.
     */
    @Override
    public boolean isDefaultResources() { // En 1.20.1, esto ayuda a la IA a ignorar obstáculos
        return true;
    }

    // Nota: Aunque en 1.20.1 esto se maneja por Tags (JSON),
    // sobreescribir estos métodos en la clase sigue funcionando para lógica forzada.

    public boolean isProjectile() {
        return true; // No knockback
    }

    public boolean isBypassArmor() {
        return true; // Ignora defensa
    }

    @Nullable
    @Override
    public Entity getDirectEntity() {
        return galath;
    }

    @Nullable
    @Override
    public Entity getEntity() {
        return galath;
    }
}