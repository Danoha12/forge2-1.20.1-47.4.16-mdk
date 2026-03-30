package com.trolmastercard.sexmod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * CummyParticleRenderer — Portado a 1.20.1.
 * * Gestiona y renderiza las partículas con física (fluidos).
 * * Usa CopyOnWriteArrayList para evitar errores de modificación concurrente.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CummyParticleRenderer {

    public static final ResourceLocation TEXTURE = new ResourceLocation("sexmod", "textures/cummy.png");
    private static final List<PhysicsParticle> PARTICLES = new CopyOnWriteArrayList<>();

    // ── Renderizado del Mundo ────────────────────────────────────────────────

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // En 1.20.1 usamos AFTER_PARTICLES para que se vean sobre el terreno pero bajo la UI
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || PARTICLES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Configuración de Shaders (Vital en 1.20.1)
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder builder = tessellator.getBuilder();
        float partialTick = event.getPartialTick();

        // Iniciamos el dibujado (Asumiendo que PhysicsParticle usa POSITION_TEX)
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (PhysicsParticle p : PARTICLES) {
            p.render(mc, tessellator, builder, partialTick);
        }

        tessellator.end();
        RenderSystem.disableBlend();
    }

    // ── Lógica de Tiempo (Ticks) ──────────────────────────────────────────────

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // Actualizamos y limpiamos partículas muertas en un solo paso
            PARTICLES.removeIf(p -> {
                p.tick();
                return p.isDead(); // Asumiendo que PhysicsParticle tiene un isDead() o lifetime <= 0
            });
        }
    }

    // ── API Estática ──────────────────────────────────────────────────────────

    public static void add(PhysicsParticle particle) {
        // Límite de seguridad: 500 partículas para no matar los FPS
        if (PARTICLES.size() < 500) {
            PARTICLES.add(particle);
        }
    }

    /** Helper para eliminar partículas vinculadas a un NPC que ha sido removido */
    public static void removeForNpc(@Nonnull BaseNpcEntity npc) {
        PARTICLES.removeIf(p -> p.getOwner() != null && p.getOwner().getUUID().equals(npc.getUUID()));
    }

    public static void clear() {
        PARTICLES.clear();
    }
}