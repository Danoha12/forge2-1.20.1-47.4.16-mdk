package com.trolmastercard.sexmod.client.renderer; // Ajusta el paquete según tu estructura

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashMap;
import java.util.Map;

/**
 * CachedAnimationProcessor — Portado a 1.20.1 / GeckoLib 4.
 * * Mantiene la compatibilidad con el código antiguo del mod al envolver el procesador
 * de animaciones estándar con un caché de búsqueda rápida para los huesos.
 */
public class CachedAnimationProcessor<T extends GeoAnimatable> extends AnimationProcessor<T> {

    private final Map<String, CoreGeoBone> boneCache = new HashMap<>();

    // En GeckoLib 4, el procesador DEBE conocer a su modelo
    public CachedAnimationProcessor(GeoModel<T> model) {
        super(model);
    }

    // =========================================================================
    //  Búsqueda con Caché
    // =========================================================================

    @Override
    public CoreGeoBone getBone(String name) {
        // Primero intentamos nuestro caché rápido
        CoreGeoBone bone = this.boneCache.get(name);
        if (bone != null) return bone;

        // Si no está (por alguna razón), dejamos que el super haga su trabajo
        return super.getBone(name);
    }

    // =========================================================================
    //  Mantenimiento del Caché
    // =========================================================================

    @Override
    public void registerBone(CoreGeoBone bone) {
        super.registerBone(bone);
        this.boneCache.put(bone.getName(), bone);
    }

    @Override
    public void clearBones() {
        super.clearBones();
        this.boneCache.clear();
    }
}