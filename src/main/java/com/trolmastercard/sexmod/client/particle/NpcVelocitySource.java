package com.trolmastercard.sexmod.client.particle;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Molde temporal para la velocidad de las partículas.
 */
public interface NpcVelocitySource {
    Vec3 getVelocity(BaseNpcEntity owner);
}