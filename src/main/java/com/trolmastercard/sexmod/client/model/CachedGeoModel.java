package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.client.renderer.CachedAnimationProcessor;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;

import java.lang.reflect.Field;

/**
 * CachedGeoModel — Portado a 1.20.1 / GeckoLib 4 y enmascarado (SFW).
 * * Clase abstracta que optimiza el rendimiento de búsqueda de huesos reemplazando
 * el procesador de animaciones estándar por un {@link CachedAnimationProcessor}.
 * * Utiliza reflexión para inyectar el procesador personalizado directamente
 * en la jerarquía de GeckoLib.
 */
public abstract class CachedGeoModel<T extends BaseNpcEntity & GeoAnimatable> extends BaseNpcModel<T> {

    protected CachedGeoModel() {
        super();
        // Inyectar el procesador con caché mediante reflexión
        try {
            // En GeckoLib 4, el campo en la clase GeoModel se llama "processor"
            Field field = findFieldInHierarchy(this.getClass(), "processor");
            if (field != null) {
                field.setAccessible(true);
                // El constructor de CachedAnimationProcessor ahora requiere el modelo (this)
                field.set(this, new CachedAnimationProcessor<>(this));
            } else {
                System.err.println("[Mod] No se pudo encontrar el campo 'processor' en la jerarquía de GeoModel.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  getBakedModel — Limpieza y registro de huesos por cada carga
    // =========================================================================

    @Override
    public BakedGeoModel getBakedModel(ResourceLocation location) {
        BakedGeoModel model = super.getBakedModel(location);
        if (model == null) {
            throw new RuntimeException("No se pudo cargar el modelo en: " + location);
        }

        // Limpiamos el caché y registramos los huesos del nuevo modelo horneado
        var processor = getAnimationProcessor();
        if (processor instanceof CachedAnimationProcessor) {
            processor.clearBones();
            for (var bone : model.topLevelBones()) {
                processor.registerBone(bone);
            }
        }

        return model;
    }

    // =========================================================================
    //  Utilidad de Reflexión
    // =========================================================================

    /**
     * Busca un campo en la clase actual y en todas sus superclases.
     */
    private Field findFieldInHierarchy(Class<?> startClass, String fieldName) {
        Class<?> current = startClass;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}