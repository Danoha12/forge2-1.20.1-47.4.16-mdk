package com.trolmastercard.sexmod.util;

import java.util.function.BiFunction;

/**
 * PhysicsParticleChain — Motor de físicas para colas, lenguas y pelo largo.
 * Maneja la configuración de segmentos encadenados.
 */
public class PhysicsParticleChain {

    /**
     * Configuración técnica para una cadena física.
     */
    public static class Config {
        public final RgbaColor color;
        public final float gravity;
        public final int segments;
        public final float segmentLength;

        // Funciones de onda para el movimiento (Seno/Coseno)
        public final BiFunction<Integer, Float, Float> waveX;
        public final BiFunction<Integer, Float, Float> waveY;
        public final BiFunction<Integer, Float, Float> waveZ;

        public final float drag;
        public final float friction;

        public Config(RgbaColor color, float gravity, int segments, float segmentLength,
                      BiFunction<Integer, Float, Float> waveX, BiFunction<Integer, Float, Float> waveY,
                      BiFunction<Integer, Float, Float> waveZ, float drag, float friction) {
            this.color = color;
            this.gravity = gravity;
            this.segments = segments;
            this.segmentLength = segmentLength;
            this.waveX = waveX;
            this.waveY = waveY;
            this.waveZ = waveZ;
            this.drag = drag;
            this.friction = friction;
        }
    }
}