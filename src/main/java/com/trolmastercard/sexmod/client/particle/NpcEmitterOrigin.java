package com.trolmastercard.sexmod.client.particle;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Molde temporal para el origen de emisión de partículas.
 */
public interface NpcEmitterOrigin {
    Vec3 getOrigin(BaseNpcEntity owner);
}