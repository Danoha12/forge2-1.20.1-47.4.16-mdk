package com.trolmastercard.sexmod.entity; // Ajusta a tu paquete de entidades

import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.entity.PartEntity;

/**
 * SelectableEntityPart — Portado a 1.20.1.
 * * Sub-entidad (hitbox secundaria) vinculada a un NPC principal.
 * * Controlada por la variable 'active' para permitir interacciones específicas.
 */
public class SelectableEntityPart extends PartEntity<BaseNpcEntity> {

    public boolean active = false;

    // 🚨 1.20.1: Las dimensiones deben guardarse en este objeto inmutable
    public final EntityDimensions partDimensions;

    public SelectableEntityPart(BaseNpcEntity parent, float width, float height) {
        super(parent);

        // Creamos las dimensiones escalables y forzamos al motor a actualizar la AABB (Bounding Box)
        this.partDimensions = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    // ── Sobrescritura de Dimensiones (Obligatorio en 1.20.1) ─────────────────

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.partDimensions;
    }

    // ── Lógica de Colisión e Interacción ─────────────────────────────────────

    @Override
    public boolean canBeCollidedWith() {
        return this.active;
    }

    @Override
    public boolean isPickable() {
        // isPickable define si el jugador puede hacerle clic derecho/izquierdo
        return this.active;
    }

    // Nota para el futuro: Recuerda que BaseNpcEntity deberá llamar a
    // part.setPos(...) en su propio método tick() para que esta hitbox
    // siga al NPC principal cuando camine.
}