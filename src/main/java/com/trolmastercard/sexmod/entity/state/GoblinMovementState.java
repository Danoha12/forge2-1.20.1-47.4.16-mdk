package com.trolmastercard.sexmod.entity.state;

/**
 * GoblinMovementState — Portado a 1.20.1.
 * * Contenedor de datos simple para rastrear las flags de movimiento y animación.
 * * Nota: Al ser un Singleton estático, ten cuidado si hay múltiples Goblins en pantalla.
 */
public class GoblinMovementState {

    // Inicializamos la instancia por defecto para evitar NullPointerExceptions
    public static GoblinMovementState INSTANCE = new GoblinMovementState(false, false, true);

    public boolean attacking;
    public boolean moving;
    public boolean idle;

    public GoblinMovementState(boolean attacking, boolean moving, boolean idle) {
        this.attacking = attacking;
        this.moving = moving;
        this.idle = idle;
    }

    /**
     * Método helper opcional para actualizar el estado rápidamente
     * sin tener que crear un nuevo objeto cada vez (mejor para el Garbage Collector).
     */
    public void update(boolean attacking, boolean moving, boolean idle) {
        this.attacking = attacking;
        this.moving = moving;
        this.idle = idle;
    }
}