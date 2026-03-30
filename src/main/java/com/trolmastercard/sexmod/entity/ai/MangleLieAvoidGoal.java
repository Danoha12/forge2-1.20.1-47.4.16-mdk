package com.trolmastercard.sexmod.entity.ai;

import com.trolmastercard.sexmod.entity.GalathEntity;
import com.trolmastercard.sexmod.entity.MangleLieEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * MangleLieAvoidGoal — Portado a 1.20.1.
 * * IA de huida para Manglelie.
 * * Se desactiva automáticamente si Manglelie ya tiene una Galath vinculada (Mommy)
 * o si hay una Galath activa en los alrededores.
 */
public class MangleLieAvoidGoal extends AvoidEntityGoal<Player> {

    private final MangleLieEntity mangleLie;
    private final float searchRadius;

    public MangleLieAvoidGoal(MangleLieEntity mob, float radius, double walkSpeed, double sprintSpeed) {
        // Configuramos la huida de jugadores dentro del radio especificado
        super(mob, Player.class, radius, walkSpeed, sprintSpeed);
        this.mangleLie = mob;
        this.searchRadius = radius;
    }

    /**
     * Verifica si la IA de huida debe ser suprimida.
     * * @return true si Manglelie se siente segura (tiene a Galath cerca o es su dueña).
     */
    private boolean isSuppressed() {
        // 1. Si ya está montada en una Galath o vinculada, no huye.
        if (this.mangleLie.getMommyUUID() != null) {
            return true;
        }

        // 2. Buscamos si hay alguna Galath salvaje o activa cerca para protegerla.
        AABB box = this.mangleLie.getBoundingBox().inflate(this.searchRadius);
        List<GalathEntity> nearbyGalaths = this.mangleLie.level().getEntitiesOfClass(GalathEntity.class, box);

        for (GalathEntity galath : nearbyGalaths) {
            // Si la Galath es válida y no está muerta/eliminada, Manglelie se queda con ella.
            if (galath.isAlive() && !galath.isRemoved()) {
                // Nota: Usamos isAlive() como reemplazo genérico de isActiveAndReady()
                return true;
            }
        }

        return false;
    }

    // ── Overrides de Goal ───────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        // Si se siente segura, ni siquiera intenta buscar un camino de huida.
        if (isSuppressed()) {
            return false;
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        // Si una Galath aparece mientras está huyendo, se detiene inmediatamente.
        if (isSuppressed()) {
            return false;
        }
        return super.canContinueToUse();
    }

    @Override
    public void start() {
        // Sincronizamos el estado visual: Manglelie pone cara de asustada/corriendo.
        // Usamos DATA_SCARED que definimos en la entidad.
        this.mangleLie.getEntityData().set(MangleLieEntity.DATA_SCARED, true);
        super.start();
    }

    @Override
    public void stop() {
        // Al dejar de huir, vuelve a su estado normal.
        this.mangleLie.getEntityData().set(MangleLieEntity.DATA_SCARED, false);
        super.stop();
    }
}