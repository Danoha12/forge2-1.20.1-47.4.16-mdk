package com.trolmastercard.sexmod.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * EntityUtil — Portado a 1.20.1.
 * * Helpers estáticos para la lógica de visión y targeting de los NPCs.
 */
public class EntityUtil {

    private EntityUtil() {}

    /**
     * Devuelve true si la entidad es un objetivo válido.
     * Se ignoran mobs explosivos, neutrales molestos o que se teletransportan.
     */
    public static boolean canTarget(Entity entity) {
        return !(entity instanceof Creeper)
                && !(entity instanceof ZombifiedPiglin)
                && !(entity instanceof Guardian)
                && !(entity instanceof EnderMan);
    }

    /**
     * Lanza un Ray-Trace desde 'from' hasta los ojos de la entidad.
     * Devuelve true si no hay bloques sólidos atravesando la línea de visión.
     */
    public static boolean hasLineOfSight(Level level, Vec3 from, Entity entity) {
        Vec3 to = entity.getEyePosition();

        BlockHitResult result = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER, // Solo chocamos con bloques sólidos
                ClipContext.Fluid.NONE,     // Ignoramos el agua/lava
                entity                      // Ignoramos a la propia entidad objetivo
        ));

        // En 1.20.1, clip() nunca es null. Si no choca con nada, su tipo es MISS.
        return result.getType() == HitResult.Type.MISS;
    }
}