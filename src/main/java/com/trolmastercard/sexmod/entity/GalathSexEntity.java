package com.trolmastercard.sexmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** GalathSexEntity stub. */
public class GalathSexEntity extends BaseNpcEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public GalathSexEntity(EntityType<? extends GalathSexEntity> type, Level level) { super(type, level); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar r) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}