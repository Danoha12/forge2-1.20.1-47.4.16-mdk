package com.trolmastercard.sexmod.util;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * PhysicsParticleSystem - ported from ep.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Manages a pool of PhysicsParticle (an) objects attached to a BaseNpcEntity.
 * Renders them as a LINE_STRIP (GL_LINE_STRIP) sorted by camera distance.
 *
 * Fields:
 *  c=30 SEGMENT_COUNT, k=6 SPAWN_PER_TICK, f=6 (unused legacy), b=0.15F spread
 *  a = target count; i = emitter origin provider (ar); d = velocity source (b8);
 *  e = owner entity; j = spread radius; h = max chain distance before break
 *
 * 1.12.2 - 1.20.1:
 *  - BufferBuilder.begin(9,...) - LINE_STRIP / LINES  (using LINE_STRIP=9)
 *  - DefaultVertexFormats.field_181706_f - DefaultVertexFormat.POSITION_COLOR
 *  - GlStateManager.func_179129_p() - RenderSystem.enableBlend()
 *  - GlStateManager.func_179118_c() - RenderSystem.defaultBlendFunc()
 *  - GlStateManager.func_179089_o() - RenderSystem.disableBlend()
 *  - func_72438_d - distanceToSqr
 *  - b6.a(Vec3,Vec3,t) - Mth.lerp / Vec3 interpolation
 *  - paramMinecraft.field_71439_g.field_70142_S etc - player.xo/yo/zo
 *  - paramMinecraft.field_71441_e - mc.level
 *  - func_181662_b.func_181669_b.func_181675_d - vertex().color().endVertex()
 */
@OnlyIn(Dist.CLIENT)
public class PhysicsParticleSystem {

    static final int   SEGMENT_COUNT  = 30;
    static final int   SPAWN_PER_TICK = 6;
    static final float SPREAD         = 0.15F;

    final List<PhysicsParticle> particles = new ArrayList<>();
    final int      targetCount;
    final NpcEmitterOrigin  emitter;
    final NpcVelocitySource velocitySource;
    final BaseNpcEntity     owner;
    final float    spreadRadius;
    final float    maxChainDist;

    public PhysicsParticleSystem(int count, NpcEmitterOrigin emitter, NpcVelocitySource velSrc,
                                  BaseNpcEntity owner, float spreadRadius, float maxChainDist) {
        this.targetCount   = count;
        this.emitter       = emitter;
        this.velocitySource= velSrc;
        this.owner         = owner;
        this.spreadRadius  = spreadRadius;
        this.maxChainDist  = maxChainDist;
    }

    /** Spawns particles if pool is short, then renders them. */
    public void render(Minecraft mc, Tesselator tess, BufferBuilder buf, float pt) {
        // Spawn up to SPAWN_PER_TICK if below target
        if (particles.size() < targetCount) {
            for (int i = 0; i < SPAWN_PER_TICK && particles.size() < targetCount; i++) {
                Vec3 origin = emitter.getOrigin(owner);
                Vec3 spread = new Vec3(
                        origin.x + (owner.getRandom().nextFloat() * 2 - 1) * spreadRadius,
                        origin.y + (owner.getRandom().nextFloat() * 2 - 1) * spreadRadius,
                        origin.z + (owner.getRandom().nextFloat() * 2 - 1) * spreadRadius);
                particles.add(new PhysicsParticle(owner.level, velocitySource.getVelocity(owner), spread));
            }
        }

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();

        // Camera position (interpolated)
        var player = mc.player;
        Vec3 camPos = new Vec3(
                net.minecraft.util.Mth.lerp(pt, player.xo, player.getX()),
                net.minecraft.util.Mth.lerp(pt, player.yo, player.getY()),
                net.minecraft.util.Mth.lerp(pt, player.zo, player.getZ()));

        buf.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        sortByDistance(camPos);

        Vec3 prev = null;
        for (PhysicsParticle p : particles) {
            Vec3 pos = new Vec3(
                    net.minecraft.util.Mth.lerp(pt, p.xPrev, p.x),
                    net.minecraft.util.Mth.lerp(pt, p.yPrev, p.y),
                    net.minecraft.util.Mth.lerp(pt, p.zPrev, p.z));
            if (prev != null && prev.distanceToSqr(pos) > maxChainDist * maxChainDist) {
                tess.end();
                buf.begin(VertexFormat.Mode.DEBUG_LINE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            }
            buf.vertex(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z)
               .color(255, 255, 255, 255)
               .endVertex();
            prev = pos;
        }
        tess.end();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    /** Advances all particles one tick. */
    public void tick() {
        for (PhysicsParticle p : particles) p.tick();
    }

    /** Insertion-sort particles by distance from camera (ascending). */
    private void sortByDistance(Vec3 cam) {
        if (particles.size() <= 1) return;
        for (int i = 1; i < particles.size(); i++) {
            PhysicsParticle key = particles.get(i);
            Vec3 keyPos = new Vec3(key.x, key.y, key.z);
            int j = i - 1;
            while (j >= 0 && cam.distanceToSqr(new Vec3(particles.get(j).x, particles.get(j).y, particles.get(j).z))
                           < cam.distanceToSqr(new Vec3(particles.get(j+1).x, particles.get(j+1).y, particles.get(j+1).z))) {
                particles.set(j + 1, particles.get(j));
                j--;
            }
            particles.set(j + 1, key);
        }
    }
}
