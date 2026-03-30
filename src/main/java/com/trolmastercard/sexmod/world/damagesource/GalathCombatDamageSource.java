package com.trolmastercard.sexmod.world.damagesource;

import com.trolmastercard.sexmod.entity.GalathEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * GalathCombatDamageSource — Portado a 1.20.1.
 * * Fuente de daño personalizada utilizada por GalathEntity.
 * * Maneja el mensaje de muerte específico: "<jugador> fue aniquilado por Galath".
 */
public class GalathCombatDamageSource extends DamageSource {

    public GalathCombatDamageSource(GalathEntity attacker) {
        // En 1.20.1 pasamos el DamageType (Mob Attack), el atacante directo y el causante original.
        super(attacker.level().registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.MOB_ATTACK),
                attacker, attacker, attacker.position());
    }

    /**
     * Define el mensaje de muerte que aparecerá en el chat.
     */
    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String victimName = victim.getName().getString();
        // Nota: En un mod profesional usarías un archivo .lang, pero aquí lo dejamos literal como tu original.
        return Component.literal(victimName + " was slain by Galath");
    }

    // Nota: Ya no necesitas sobreescribir getEntity() ni getSourcePosition()
    // porque la clase base DamageSource ya los guarda y devuelve correctamente
    // gracias a que los pasamos en el constructor 'super'.
}