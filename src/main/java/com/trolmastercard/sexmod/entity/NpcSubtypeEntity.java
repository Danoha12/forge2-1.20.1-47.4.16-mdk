package com.trolmastercard.sexmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Entidad base para subtipos de NPCs (variantes).
 */
public class NpcSubtypeEntity extends BaseNpcEntity {
    private final AnimatableInstanceCache animCache = GeckoLibUtil.createInstanceCache(this);

    public NpcSubtypeEntity(EntityType<? extends BaseNpcEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animCache;
    }
}