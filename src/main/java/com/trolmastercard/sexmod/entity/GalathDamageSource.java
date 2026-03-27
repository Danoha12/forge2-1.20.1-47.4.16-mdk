package com.trolmastercard.sexmod.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * GalathDamageSource - Fuente de Daño por Drenaje de Galath.
 * Portado a 1.20.1.
 * * Usado cuando Galath drena la salud/fuerza vital del jugador.
 * * Produce un mensaje de muerte personalizado.
 * * * * NOTA 1.20.1: isProjectile() y isBypassArmor() fueron eliminados por Mojang.
 * * Para simular la perforación de armadura original, se usa DamageTypes.MAGIC.
 */
public class GalathDamageSource extends DamageSource {

    private final GalathEntity galath;
    private final Vec3 damagePosition;

    // =========================================================================
    //  Constructor (Vinculado a Daño Mágico para ignorar armadura)
    // =========================================================================

    public GalathDamageSource(GalathEntity galath) {
        // En 1.20.1 vinculamos directamente al tipo MAGIC de Vanilla.
        // Esto automáticamente ignora la armadura como se pretendía en la 1.12.2.
        super(galath.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.MAGIC),
                galath, galath);

        this.galath = galath;
        this.damagePosition = galath.position();
    }

    // =========================================================================
    //  Mensajes y Datos del Ataque
    // =========================================================================

    /** Mensaje de muerte personalizado cuando el jugador es drenado. */
    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        return Component.literal(victim.getName().getString()
                + " was drained of their life force by Galath");
    }

    /**
     * La causa inmediata del daño.
     */
    @Nullable
    @Override
    public Entity getDirectEntity() {
        return galath;
    }

    /**
     * La causa raíz del daño.
     */
    @Nullable
    @Override
    public Entity getEntity() {
        return galath;
    }

    /**
     * La posición en el mundo donde se originó el daño.
     */
    @Nullable
    @Override
    public Vec3 getSourcePosition() {
        return damagePosition;
    }
}