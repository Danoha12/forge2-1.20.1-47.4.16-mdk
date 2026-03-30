package com.trolmastercard.sexmod.client.particle; // Ajusta a tu paquete de partículas

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DragonBreathParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * SexmodDragonBreathParticle — Portado a 1.20.1.
 * * Partícula personalizada basada en el Aliento de Dragón.
 * * Utiliza un multiplicador global de tamaño estático.
 */
@OnlyIn(Dist.CLIENT)
public class SexmodDragonBreathParticle extends DragonBreathParticle {

    public static final float DEFAULT_SIZE = 0.2F;
    public static final float HALF_SIZE_CAP = 0.5F;

    /**
     * 🚨 ADVERTENCIA: Escalar dinámicamente este valor afectará a TODAS
     * las instancias vivas de esta partícula simultáneamente en la pantalla.
     */
    public static float SIZE = 0.2F;

    // ── Constructor (Actualizado a 1.20.1) ───────────────────────────────────

    public SexmodDragonBreathParticle(ClientLevel level, double x, double y, double z,
                                      double dx, double dy, double dz, SpriteSet sprites) {
        // En 1.20.1, DragonBreathParticle pide el SpriteSet directamente en el super()
        super(level, x, y, z, dx, dy, dz, sprites);
    }

    // ── Lógica de Renderizado ────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Forzamos el tamaño del Quad usando el valor estático en cada frame
        this.quadSize = 0.1F * SIZE;
    }

    // ── Proveedor / Fábrica ──────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double dx, double dy, double dz) {

            // Instanciamos pasándole las velocidades y el SpriteSet correctamente
            return new SexmodDragonBreathParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}