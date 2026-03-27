package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.BaseNpcEntity;
import com.trolmastercard.sexmod.client.anim.CachedAnimationProcessor;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

import java.lang.reflect.Field;

/**
 * CachedGeoModel - Sistema de Optimización de Modelos.
 * Portado a 1.20.1 / GeckoLib 4.
 * * Esta clase abstracta reemplaza el procesador de animaciones por defecto por uno
 * con caché (CachedAnimationProcessor) para acelerar la búsqueda de huesos.
 * Utiliza reflexión para mantener la compatibilidad con el núcleo de GeckoLib.
 */
public abstract class CachedGeoModel<T extends BaseNpcEntity & GeoAnimatable>
        extends BaseNpcModel<T> {

    protected CachedGeoModel() {
        super();
        // Intercambio del procesador por defecto por la versión optimizada con caché
        try {
            // Buscamos el campo en la clase base de GeckoLib (GeoModel)
            Field field = findFieldInHierarchy(GeoModel.class, "animationProcessor");
            if (field != null) {
                field.setAccessible(true);
                // Inyectamos nuestro procesador personalizado
                field.set(this, new CachedAnimationProcessor<T>());
            }
        } catch (Exception e) {
            // Si la reflexión falla, el mod usará el procesador estándar (más lento pero seguro)
            e.printStackTrace();
        }
    }

    // =========================================================================
    //  getBakedModel - Limpia y re-registra los huesos en cada carga
    // =========================================================================

    @Override
    public BakedGeoModel getBakedModel(ResourceLocation location) {
        BakedGeoModel model = super.getBakedModel(location);
        if (model == null) {
            throw new RuntimeException("Error crítico: No se encontró el modelo en la ruta: " + location);
        }

        // Accedemos al procesador para refrescar la jerarquía de huesos
        var processor = getAnimationProcessor();

        // Si el procesador es de nuestro tipo Cached, limpiamos la memoria vieja
        if (processor instanceof CachedAnimationProcessor) {
            ((CachedAnimationProcessor<T>) processor).clearBones();
            for (var bone : model.topLevelBones()) {
                ((CachedAnimationProcessor<T>) processor).registerBone(bone);
            }
        }

        return model;
    }

    // =========================================================================
    //  Ayudante de Reflexión (Búsqueda en profundidad)
    // =========================================================================

    /**
     * Busca un campo específico subiendo por la jerarquía de clases.
     * Esto es necesario porque GeckoLib 4 ha cambiado la estructura interna.
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