package com.trolmastercard.sexmod.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.DragonBreathParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Custom dragon-breath style particle used by sexmod effects.
 * Ported from ez.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Extends vanilla {@link DragonBreathParticle} and exposes a static
 * {@code SIZE} field that the mod adjusts to resize all live particles.
 */
@OnlyIn(Dist.CLIENT)
public class SexmodDragonBreathParticle extends DragonBreathParticle {

    /** Default alpha/size multiplier (original {@code a = 0.2F}). */
    public static final float DEFAULT_SIZE = 0.2F;

    /** Half-size cap used for clipping (original {@code c = 0.5F}). */
    public static final float HALF_SIZE_CAP = 0.5F;

    /**
     * Dynamic global size scalar - written by the mod to resize all instances
     * at runtime (original static field {@code b}, default 0.2F).
     */
    public static float SIZE = 0.2F;

    public SexmodDragonBreathParticle(ClientLevel level,
                                      double x, double y, double z) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
    }

    /**
     * Renders the particle; overrides the parent to honour the dynamic
     * {@link #SIZE} field every frame.
     *
     * In 1.20.1 the DragonBreathParticle render path is handled by the
     * billboard render of its parent {@code TextureSheetParticle}.
     * We simply push the dynamic scale into {@code quadSize} each tick.
     */
    @Override
    public void tick() {
        super.tick();
        // Synchronise the quad size with the global dynamic value
        this.quadSize = 0.1F * SIZE;
    }

    // -- Provider -------------------------------------------------------------

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
            SexmodDragonBreathParticle p = new SexmodDragonBreathParticle(level, x, y, z);
            p.pickSprite(sprites);
            return p;
        }
    }
}
