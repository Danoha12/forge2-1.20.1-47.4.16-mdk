package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.BaseNpcEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraftforge.entity.PartEntity;

/**
 * SelectableEntityPart - Portado de bb.class (1.12.2) a 1.20.1.
 * * Parte de entidad multi-parte cuya detección de colisión y selección (pickability)
 * depende del flag 'active'.
 */
public class SelectableEntityPart extends PartEntity<BaseNpcEntity> {

    public boolean active = false;
    private final EntityDimensions dimensions;

    /**
     * @param parent El NPC dueño de esta parte.
     * @param width  Ancho de la hitbox.
     * @param height Alto de la hitbox.
     */
    public SelectableEntityPart(BaseNpcEntity parent, float width, float height) {
        super(parent);
        this.dimensions = EntityDimensions.scalable(width, height);
        this.refreshDimensions();
    }

    @Override
    public boolean isPickable() {
        return this.active;
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.active;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.dimensions;
    }
}