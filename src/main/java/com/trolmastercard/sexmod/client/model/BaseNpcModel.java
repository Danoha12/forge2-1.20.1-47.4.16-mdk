package com.trolmastercard.sexmod.client.model;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.HashSet;
import java.util.Optional;

/**
 * BaseNpcModel - ported from cv.class (Fapcraft 1.12.2 v1.1) to 1.20.1 / GeckoLib 4.
 *
 * Abstract parent GeoModel for all NPC entities.
 * Each subclass provides:
 *   - {@link #getGeoFiles()}  - array of geo ResourceLocations (indexed by MODEL_INDEX)
 *   - {@link #getTextureResource(BaseNpcEntity)} - default texture path
 *   - {@link #getAnimationResource(BaseNpcEntity)} - animation json path
 *   - Bone-name arrays for each clothing slot (helmet, chest, upper flesh, etc.)
 *
 * The protected {@code c} array (geo files) is populated by {@link #getGeoFiles()}.
 *
 * ============================================================
 * Slot bone arrays (override in subclasses):
 * ============================================================
 *   c() - getHelmetBones()
 *   f() - getChestBones()
 *   a() - getUpperFleshBones()
 *   h() - getLowerArmorBones()
 *   e() - getLowerFleshBones()
 *   b() - getShoeBones()
 *   g() - getFeatureBones()   (optional: feelers, leaves, big-blob, etc.)
 *
 * ============================================================
 * GeckoLib 3 - 4:
 * ============================================================
 *   AnimatedGeoModel<T>           - GeoModel<T>
 *   AnimationEvent<T>             - AnimationState<T>
 *   IBone                         - CoreGeoBone
 *   clearModelRendererList()      - clearBones() via CachedAnimationProcessor
 */
public abstract class BaseNpcModel<T extends BaseNpcEntity> extends GeoModel<T> {

    /** Geo file array, populated once from {@link #getGeoFiles()}. */
    protected final ResourceLocation[] c;

    protected BaseNpcModel() {
        this.c = getGeoFiles();
    }

    // =========================================================================
    //  Abstract / overridable geometry selectors
    // =========================================================================

    /** Returns the array of geo resource locations this model can use. */
    protected abstract ResourceLocation[] getGeoFiles();

    /** Default model selection: uses MODEL_INDEX to index into {@link #c}. */
    @Override
    public ResourceLocation getModelResource(T entity) {
        if (c == null || c.length == 0) return null;
        int idx = entity.getEntityData().get(BaseNpcEntity.MODEL_INDEX);
        if (idx < 0 || idx >= c.length) return c[0];
        return c[idx];
    }

    // =========================================================================
    //  setCustomAnimations  (original: cv.a(em, Integer, AnimationEvent))
    // =========================================================================

    @Override
    public void setCustomAnimations(T entity, long instanceId, AnimationState<T> animState) {
        // Base: no-op. Subclasses call super() first, then do their own logic.
    }

    // =========================================================================
    //  Slot bone arrays (override in subclasses)
    // =========================================================================

    /** Helmet bone names.        Original: {@code cv.c()} */
    public String[] getHelmetBones()      { return new String[0]; }
    /** Chest-armor bone names.   Original: {@code cv.f()} */
    public String[] getChestBones()       { return new String[0]; }
    /** Upper flesh bone names.   Original: {@code cv.a()} */
    public String[] getUpperFleshBones()  { return new String[0]; }
    /** Lower armor bone names.   Original: {@code cv.h()} */
    public String[] getLowerArmorBones()  { return new String[0]; }
    /** Lower flesh bone names.   Original: {@code cv.e()} */
    public String[] getLowerFleshBones()  { return new String[0]; }
    /** Shoe bone names.          Original: {@code cv.b()} */
    public String[] getShoeBones()        { return new String[0]; }
    /** Optional extra feature bones (feelers, bigblob, etc.). Original: {@code cv.g()} */
    public String[] getFeatureBones()     { return new String[0]; }

    // =========================================================================
    //  Utility helpers
    // =========================================================================

    /** Returns true if entity's current AnimState is one of {@code states}. */
    public static boolean isInState(BaseNpcEntity entity, AnimState... states) {
        AnimState cur = entity.getAnimState();
        for (AnimState s : states) {
            if (cur == s) return true;
        }
        return false;
    }

    public HashSet<String> getHiddenBoneNames() {
        return NpcBoneRegistry.EMPTY_SET;
    }

    protected Optional<GeoBone> getBone(String name) {
        return getAnimationProcessor().getBone(name);
    }

    protected void setBoneHidden(String name, boolean hidden) {
        CoreGeoBone bone = getBone(name);
        if (bone != null) bone.setHidden(hidden);
    }
}
