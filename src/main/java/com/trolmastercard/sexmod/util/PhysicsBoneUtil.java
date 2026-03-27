package com.trolmastercard.sexmod.util;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * PhysicsBoneUtil - breast/skirt physics bone deflection utility.
 * Ported from gx.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Original obfuscation:
 *   gx         - PhysicsBoneUtil
 *   gx.a       - hiddenBoneSet (HashSet<String>)
 *   gx.b       - currentVelocity (Vec3)
 *   gx.c       - POSITIVE_DEFLECT (Vec3 scale)
 *   gx.e       - NEGATIVE_DEFLECT (Vec3 scale)
 *   gx.f       - MAX_DEFLECT_SCALE
 *   gx.d       - boneFilterCache (HashMap)
 *   c3         - IBoneFilter
 *   ck.a()     - VectorMathUtil.dot(Vector3f, Vec3)
 *   b6.e()     - MathUtil.abs(double)
 *   b6.a()     - MathUtil.lerp(Vec3, Vec3, double)
 *   cj.a()     - BoneMatrixUtil.getEntityVelocity(entity, partialTick)
 *
 * GeckoLib migration:
 *   GeckoLib3 IBone/GeoBone - GeckoLib4 CoreGeoBone
 *   IBone.getName()         - CoreGeoBone.getName()  (unchanged)
 */
public class PhysicsBoneUtil {

    public static final Vec3 POSITIVE_DEFLECT = new Vec3(0.95, 0.65, 0.85);
    public static final Vec3 NEGATIVE_DEFLECT = new Vec3(0.0,  0.2,  0.3);
    public static final float MAX_DEFLECT_SCALE = 0.1f;

    /** Bone names that participate in physics simulation. */
    public static final HashSet<String> PHYSICS_BONES = createPhysicsBoneSet();

    /** Per-filter cache of per-bone visibility. */
    protected static HashMap<IBoneFilter, HashMap<String, Boolean>> boneFilterCache = new HashMap<>();

    /** Current entity velocity Vec3, set each frame by setEntityVelocity(). */
    public static Vec3 currentVelocity;

    // -------------------------------------------------------------------------

    private static HashSet<String> createPhysicsBoneSet() {
        HashSet<String> s = new HashSet<>();
        s.add("boobs");
        s.add("booty");
        s.add("vagina");
        s.add("fuckhole");
        return s;
    }

    /**
     * Returns (and caches) whether {@code bone} passes the filter for the given renderer.
     */
    static boolean passesFilter(IBoneFilter filter, CoreGeoBone bone) {
        HashMap<String, Boolean> cache = boneFilterCache.get(filter);
        if (cache == null) {
            cache = new HashMap<>();
            boolean result = filter.shouldAffect(filter.getFilterSet(), bone);
            cache.put(bone.getName(), result);
            boneFilterCache.put(filter, cache);
            return result;
        }
        Boolean cached = cache.get(bone.getName());
        if (cached == null) {
            cached = filter.shouldAffect(filter.getFilterSet(), bone);
            cache.put(bone.getName(), cached);
            boneFilterCache.put(filter, cache);
        }
        return cached;
    }

    /**
     * Applies physics deflection to {@code bonePos} based on the current entity velocity.
     * Returns the (possibly deflected) position.
     */
    public static Vec3 applyPhysics(IBoneFilter filter, CoreGeoBone bone,
                                    Vec3 bonePos, Vector3f boneNormal) {
        if (!passesFilter(filter, bone)) return bonePos;
        return computeDeflection(bonePos, boneNormal, currentVelocity);
    }

    /**
     * Computes deflection of {@code pos} based on velocity dot-product against {@code normal}.
     */
    public static Vec3 computeDeflection(Vec3 pos, Vector3f normal, Vec3 velocity) {
        double dot    = VectorMathUtil.dot(normal, velocity);
        double scale  = Math.abs(dot) * MAX_DEFLECT_SCALE;
        return MathUtil.lerp(pos, (dot > 0.0) ? POSITIVE_DEFLECT : NEGATIVE_DEFLECT, scale);
    }

    /** Called once per frame to cache the entity's current velocity. */
    public static void setEntityVelocity(BaseNpcEntity entity, float partialTick) {
        currentVelocity = BoneMatrixUtil.getEntityVelocity(entity, partialTick);
    }

    /**
     * Pre-populates the filter cache for all bones in a list.
     * Called once when a renderer first encounters a new filter instance.
     */
    public static void prewarmCache(List<CoreGeoBone> bones, HashSet<String> filterSet,
                                    IBoneFilter filter) {
        if (boneFilterCache.get(filter) != null) return;
        HashMap<String, Boolean> cache = new HashMap<>();
        for (CoreGeoBone bone : bones) {
            cache.put(bone.getName(), filter.shouldAffect(filterSet, bone));
        }
        boneFilterCache.put(filter, cache);
    }
}
