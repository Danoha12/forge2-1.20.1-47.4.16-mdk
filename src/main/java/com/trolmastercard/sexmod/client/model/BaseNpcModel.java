package com.trolmastercard.sexmod.client.model;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;

/**
 * BaseNpcModel — Portado a 1.20.1 / GeckoLib 4.
 * * Modelo abstracto padre para todas las entidades NPC.
 * * Maneja variaciones de geometría, huesos de ropa y utilidades de renderizado.
 */
public abstract class BaseNpcModel<T extends BaseNpcEntity> extends GeoModel<T> {

    /** Caché de archivos Geo. Poblado de forma perezosa para evitar fugas de memoria. */
    protected ResourceLocation[] geoFilesCache;

    protected BaseNpcModel() {
        // No llamamos a métodos abstractos aquí. Se inicializará la primera vez que se pida.
    }

    // =========================================================================
    //  Selectores de Geometría
    // =========================================================================

    /** Retorna el arreglo de ubicaciones de recursos geo que este modelo puede usar. */
    protected abstract ResourceLocation[] getGeoFiles();

    @Override
    public ResourceLocation getModelResource(T entity) {
        // Lazy loading seguro: solo cargamos el array la primera vez
        if (geoFilesCache == null) {
            geoFilesCache = getGeoFiles();
        }

        if (geoFilesCache == null || geoFilesCache.length == 0) return null;

        // En 1.20.1, acceder a los EntityData es rápido, pero asegúrate de que MODEL_INDEX esté registrado
        int idx = entity.getEntityData().get(BaseNpcEntity.MODEL_INDEX);

        if (idx < 0 || idx >= geoFilesCache.length) return geoFilesCache[0];

        return geoFilesCache[idx];
    }

    // Nota: Como heredas de GeoModel, las subclases ESTÁN OBLIGADAS a implementar:
    // public abstract ResourceLocation getTextureResource(T entity);
    // public abstract ResourceLocation getAnimationResource(T entity);

    // =========================================================================
    //  Animaciones Procedurales (GeckoLib 4)
    // =========================================================================

    @Override
    public void setCustomAnimations(T entity, long instanceId, AnimationState<T> animState) {
        super.setCustomAnimations(entity, instanceId, animState);
        // Base: no-op. Las subclases como JennyModel o EllieModel inyectarán rotaciones
        // de cabeza, parpadeos de ojos o físicas de pechos aquí.
    }

    // =========================================================================
    //  Arreglos de Huesos por Ranura (Ropa / Cuerpo)
    // =========================================================================

    public String[] getHelmetBones()      { return new String[0]; }
    public String[] getChestBones()       { return new String[0]; }
    public String[] getUpperFleshBones()  { return new String[0]; }
    public String[] getLowerArmorBones()  { return new String[0]; }
    public String[] getLowerFleshBones()  { return new String[0]; }
    public String[] getShoeBones()        { return new String[0]; }
    public String[] getFeatureBones()     { return new String[0]; }

    // =========================================================================
    //  Integración con Equipamiento (¡Nuevo para ColoredNpcHandRenderer!)
    // =========================================================================

    /**
     * Determina qué objeto de armadura corresponde a un hueso específico.
     * Sobrescribir en subclases si el NPC soporta armadura visible.
     */
    public ItemStack getArmorItemForBone(T entity, String boneName) {
        return ItemStack.EMPTY; // Por defecto, sin armadura.
    }

    // =========================================================================
    //  Helpers de Utilidad
    // =========================================================================

    /** Verifica rápido si la entidad está en uno de los estados especificados. */
    public static boolean isInState(BaseNpcEntity entity, AnimState... states) {
        AnimState cur = entity.getAnimState();
        for (AnimState s : states) {
            if (cur == s) return true;
        }
        return false;
    }

    public HashSet<String> getHiddenBoneNames() {
        return new HashSet<>();
    }

    /**
     * Oculta o muestra un hueso en GeckoLib 4.
     */
    protected void setBoneHidden(String name, boolean hidden) {
        // En GL4, getBone() devuelve GeoBone directamente (o null si no existe)
        GeoBone bone = this.getAnimationProcessor().getBone(name);
        if (bone != null) {
            bone.setHidden(hidden);
        }
    }
}