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
    //  Búsqueda con Caché (Actualizado y Automático)
    // =========================================================================

    @Override
    public CoreGeoBone getBone(String name) {
        // Primero intentamos nuestro caché rápido
        CoreGeoBone cachedBone = this.boneCache.get(name);
        if (cachedBone != null) return cachedBone;

        // Si no está, dejamos que el sistema original de GeckoLib 4 lo busque
        CoreGeoBone bone = super.getBone(name);

        // Si lo encontró, lo guardamos en nuestro caché para la próxima vez
        if (bone != null) {
            this.boneCache.put(name, bone);
        }

        return bone;
    }

    // =========================================================================
    //  Mantenimiento del Caché (Sin @Override ni super para no crashear)
    // =========================================================================

    // Se mantiene por si el mod antiguo lo llama, pero ya solo afecta a nuestro caché
    public void registerBone(CoreGeoBone bone) {
        this.boneCache.put(bone.getName(), bone);
    }

    // Se mantiene por si el mod antiguo lo llama, pero ya solo afecta a nuestro caché
    public void clearBones() {
        this.boneCache.clear();
    }
}