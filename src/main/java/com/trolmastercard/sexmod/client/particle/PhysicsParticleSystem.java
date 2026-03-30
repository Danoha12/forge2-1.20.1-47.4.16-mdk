package com.trolmastercard.sexmod.client.particle; // Ajusta al paquete correcto

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * PhysicsParticleSystem — Portado a 1.20.1.
 * * Sistema de partículas unidas renderizadas como LINE_STRIP.
 * * Optimizador de memoria y shaders actualizados.
 */
@OnlyIn(Dist.CLIENT)
public class PhysicsParticleSystem {

    static final int SEGMENT_COUNT = 30;
    static final int SPAWN_PER_TICK = 6;
    static final float SPREAD = 0.15F;

    final List<PhysicsParticle> particles = new ArrayList<>();
    final int targetCount;
    final NpcEmitterOrigin emitter;
    final NpcVelocitySource velocitySource;
    final BaseNpcEntity owner;
    final float spreadRadius;
    final float maxChainDist;

    public PhysicsParticleSystem(int count, NpcEmitterOrigin emitter, NpcVelocitySource velSrc,
                                 BaseNpcEntity owner, float spreadRadius, float maxChainDist) {
        this.targetCount = count;
        this.emitter = emitter;
        this.velocitySource = velSrc;
        this.owner = owner;
        this.spreadRadius = spreadRadius;
        this.maxChainDist = maxChainDist;
    }

    // ── Lógica de Renderizado ────────────────────────────────────────────────

    public void render(Minecraft mc, Tesselator tess, BufferBuilder buf, float pt) {
        // Generar partículas si faltan
        if (this.particles.size() < this.targetCount) {
            for (int i = 0; i < SPAWN_PER_TICK && this.particles.size() < this.targetCount; i++) {
                Vec3 origin = this.emitter.getOrigin(this.owner);
                Vec3 spread = new Vec3(
                        origin.x + (this.owner.getRandom().nextFloat() * 2 - 1) * this.spreadRadius,
                        origin.y + (this.owner.getRandom().nextFloat() * 2 - 1) * this.spreadRadius,
                        origin.z + (this.owner.getRandom().nextFloat() * 2 - 1) * this.spreadRadius
                );
                // Asumo que PhysicsParticle ahora recibe Vec3 en su constructor
                this.particles.add(new PhysicsParticle(this.owner.level(), this.velocitySource.getVelocity(this.owner), spread));
            }
        }

        if (this.particles.isEmpty()) return;

        // 🚨 1.20.1: Preparación estricta de Shaders y Blend
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Desactivamos el cull para que las líneas se vean desde cualquier ángulo
        RenderSystem.disableCull();

        // Posición de la cámara (Seguro para 3ra persona)
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        // Ordenamiento nativo optimizado (de lejos a cerca)
        this.particles.sort((p1, p2) -> {
            double d1 = camPos.distanceToSqr(p1.x, p1.y, p1.z);
            double d2 = camPos.distanceToSqr(p2.x, p2.y, p2.z);
            return Double.compare(d2, d1); // Orden descendente
        });

        buf.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        Vec3 prev = null;
        double maxChainSqr = this.maxChainDist * this.maxChainDist;

        for (PhysicsParticle p : this.particles) {
            // LERP manual de la partícula
            double px = net.minecraft.util.Mth.lerp(pt, p.xPrev, p.x);
            double py = net.minecraft.util.Mth.lerp(pt, p.yPrev, p.y);
            double pz = net.minecraft.util.Mth.lerp(pt, p.zPrev, p.z);

            Vec3 pos = new Vec3(px, py, pz);

            // Si la distancia entre partículas rompe la cadena, dibujamos y empezamos una nueva línea
            if (prev != null && prev.distanceToSqr(pos) > maxChainSqr) {
                tess.end();
                buf.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            }

            // Las coordenadas del Buffer deben ser relativas a la cámara
            buf.vertex(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)
                    .color(255, 255, 255, 255)
                    .endVertex();

            prev = pos;
        }

        tess.end();

        // Restaurar estado de OpenGL
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ── Lógica de Actualización ──────────────────────────────────────────────

    public void tick() {
        // En Java moderno, removeIf es más seguro si las partículas "mueren"
        // this.particles.removeIf(PhysicsParticle::isDead);
        for (PhysicsParticle p : this.particles) {
            p.tick();
        }
    }
}