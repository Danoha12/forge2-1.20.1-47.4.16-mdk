package com.trolmastercard.sexmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * PyroEntity — La entidad easter-egg.
 * No necesita GeckoLib porque se renderiza como un billboard 2D.
 */
public class PyroEntity extends Entity {

    // El Renderer busca este campo para la animación de "inflado"
    public int fatStartTick = -1;

    public PyroEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        // No necesita datos sincronizados complejos por ahora
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("fatStartTick")) this.fatStartTick = tag.getInt("fatStartTick");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("fatStartTick", this.fatStartTick);
    }
}