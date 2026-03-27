package com.trolmastercard.sexmod.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * GalathCombatDamageSource - Fuente de Daño de Combate de Galath.
 * Portado a 1.20.1.
 * * Un DamageSource personalizado usado por GalathEntity durante el combate.
 * * Almacena la entidad atacante y la posición del impacto.
 * * Genera el mensaje de muerte: "<víctima> fue asesinado por Galath"
 */
public class GalathCombatDamageSource extends DamageSource {

    private final GalathEntity attacker;
    private final Vec3 hitPos;

    // =========================================================================
    //  Constructor (Adaptado al sistema de Registros de 1.20.1)
    // =========================================================================

    public GalathCombatDamageSource(GalathEntity attacker) {
        super(attacker.level().registryAccess()
                        .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK),
                attacker, attacker);

        this.attacker = attacker;
        this.hitPos   = attacker.position();
    }

    // =========================================================================
    //  Mensajes y Datos del Ataque
    // =========================================================================

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        return Component.literal(victim.getName().getString() + " was slain by Galath");
    }

    @Override
    @Nullable
    public Entity getDirectEntity() {
        return attacker;
    }

    @Override
    @Nullable
    public Entity getEntity() {
        return attacker;
    }

    @Override
    @Nullable
    public Vec3 getSourcePosition() {
        return hitPos;
    }
}