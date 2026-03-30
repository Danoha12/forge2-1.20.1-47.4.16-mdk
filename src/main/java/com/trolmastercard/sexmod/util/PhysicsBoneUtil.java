package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone; // Paquete moderno de GeckoLib 4

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PhysicsBoneUtil — Portado a 1.20.1.
 * * Utilidad de deflexión y físicas para huesos específicos.
 * * Calcula el rebote basado en la velocidad y la normal del hueso.
 */
public class PhysicsBoneUtil {

    public static final Vec3 POSITIVE_DEFLECT = new Vec3(0.95, 0.65, 0.85);
    public static final Vec3 NEGATIVE_DEFLECT = new Vec3(0.0, 0.2, 0.3);
    public static final float MAX_DEFLECT_SCALE = 0.1f;

    // 🚨 1.20.1: Set inmutable ultra rápido para los huesos con físicas.
    public static final Set<String> PHYSICS_BONES = Set.of(
            "boobs", "booty", "vagina", "fuckhole"
    );

    // 🛡️ ESCUDO DE MEMORIA: WeakHashMap permite que el Garbage Collector elimine
    // los IBoneFilter que ya no se usan, evitando que el juego se quede sin RAM.
    protected static final Map<IBoneFilter, Map<String, Boolean>> boneFilterCache = new WeakHashMap<>();

    // Ojo: Usar variables estáticas para estados por frame puede ser peligroso
    // si usas mods como Oculus/Optifine, pero para Minecraft base está bien.
    public static Vec3 currentVelocity = Vec3.ZERO;

    // ── Lógica de Filtrado (Caché seguro) ────────────────────────────────────

    static boolean passesFilter(IBoneFilter filter, CoreGeoBone bone) {
        // computeIfAbsent es mucho más limpio que los if/else nulos de Java 8
        Map<String, Boolean> cache = boneFilterCache.computeIfAbsent(filter, k -> new ConcurrentHashMap<>());

        return cache.computeIfAbsent(bone.getName(), k -> filter.shouldAffect(filter.getFilterSet(), bone));
    }

    // ── Lógica de Físicas ────────────────────────────────────────────────────

    public static Vec3 applyPhysics(IBoneFilter filter, CoreGeoBone bone, Vec3 bonePos, Vector3f boneNormal) {
        if (!passesFilter(filter, bone)) return bonePos;
        return computeDeflection(bonePos, boneNormal, currentVelocity);
    }

    public static Vec3 computeDeflection(Vec3 pos, Vector3f normal, Vec3 velocity) {
        // Asumiendo que VectorMathUtil maneja JOML Vector3f contra Minecraft Vec3
        double dot = VectorMathUtil.dot(normal, velocity);
        double scale = Math.abs(dot) * MAX_DEFLECT_SCALE;

        Vec3 targetDeflect = (dot > 0.0) ? POSITIVE_DEFLECT : NEGATIVE_DEFLECT;

        // 1.20.1: Vec3.lerp() es nativo
        return pos.lerp(targetDeflect, scale);
    }

    // ── Actualización por Frame ──────────────────────────────────────────────

    public static void setEntityVelocity(BaseNpcEntity entity, float partialTick) {
        currentVelocity = BoneMatrixUtil.getEntityVelocity(entity, partialTick);
    }

    public static void prewarmCache(Iterable<CoreGeoBone> bones, Set<String> filterSet, IBoneFilter filter) {
        if (boneFilterCache.containsKey(filter)) return;

        Map<String, Boolean> cache = new ConcurrentHashMap<>();
        for (CoreGeoBone bone : bones) {
            cache.put(bone.getName(), filter.shouldAffect(filterSet, bone));
        }
        boneFilterCache.put(filter, cache);
    }
}