package com.trolmastercard.sexmod.client.anim;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.HashMap;

/**
 * CachedAnimationProcessor - Sistema de Cache de Huesos.
 * Portado a 1.20.1 / GeckoLib 4.
 * * Esta clase extiende el procesador de animaciones estándar para añadir un
 * sistema de búsqueda rápida basado en un HashMap. Esto evita búsquedas lineales
 * repetitivas en modelos con muchos huesos, mejorando el rendimiento (FPS).
 */
public class CachedAnimationProcessor<T extends GeoAnimatable>
        extends AnimationProcessor<T> {

    /** Mapa de caché para acceso instantáneo a los huesos por nombre. */
    private final HashMap<String, CoreGeoBone> boneCache = new HashMap<>();

    public CachedAnimationProcessor() {
        super();
    }

    // =========================================================================
    //  getBone - Búsqueda optimizada por Caché
    // =========================================================================

    @Override
    public CoreGeoBone getBone(String name) {
        // En lugar de buscar en toda la lista, preguntamos directamente al mapa
        return boneCache.get(name);
    }

    // =========================================================================
    //  Mantenimiento del Caché (Registro y Limpieza)
    // =========================================================================

    /**
     * Registra un hueso en el procesador y lo añade simultáneamente al caché.
     */
    @Override
    public void registerBone(CoreGeoBone bone) {
        super.registerBone(bone);
        // Guardamos el hueso usando su nombre como clave
        boneCache.put(bone.getName(), bone);
    }

    /**
     * Limpia la lista de huesos del procesador y vacía el caché de memoria.
     */
    @Override
    public void clearBones() {
        super.clearBones();
        boneCache.clear();
    }
}