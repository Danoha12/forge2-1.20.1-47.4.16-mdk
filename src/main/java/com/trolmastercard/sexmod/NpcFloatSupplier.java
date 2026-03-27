package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;

/**
 * NpcFloatSupplier - functional interface supplying a float value from an NPC entity.
 * Ported from gt.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 */
@FunctionalInterface
public interface NpcFloatSupplier {
    float get(BaseNpcEntity npc);
}
