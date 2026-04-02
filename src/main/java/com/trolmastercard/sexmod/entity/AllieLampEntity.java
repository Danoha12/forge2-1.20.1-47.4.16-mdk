package com.trolmastercard.sexmod.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * AllieLampEntity — Esqueleto portado a 1.20.1 / GeckoLib 4.
 * * Entidad que representa la lámpara de Allie.
 */
public class AllieLampEntity extends PathfinderMob implements GeoEntity {

    // Caché requerida por GeckoLib 4 para manejar las animaciones
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AllieLampEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    // ── GeckoLib 4 Controllers ───────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        // Aquí puedes añadir controladores de animación si la lámpara se mueve o flota
        // Ejemplo: registrar.add(new AnimationController<>(this, "controller", 0, state -> PlayState.CONTINUE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}