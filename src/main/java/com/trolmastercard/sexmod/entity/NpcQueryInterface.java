package com.trolmastercard.sexmod.entity;

/**
 * NpcQueryInterface - Sistema de Consultas de la Tribu.
 * Portado a 1.20.1.
 * * Expone los estados booleanos y el acceso al objetivo de combate para las entidades NPC
 * que participan en las mecánicas de defensa de la tribu o secuencias de ataque.
 */
public interface NpcQueryInterface {

    /** * Devuelve el objetivo de combate actual del NPC.
     * @return El KoboldEntity que está siendo atacado, o null si está pacífico.
     */
    KoboldEntity getCombatTarget();

    /** * @return true si el NPC está en modo de guardia o defendiendo la aldea/tribu.
     */
    boolean isDefending();

    /** * @return true si este NPC está en alerta máxima (ha detectado una amenaza).
     */
    boolean isAlarmed();

    /** * @return true si el NPC está en plena secuencia de ataque activo.
     */
    boolean isAttacking();
}