package com.trolmastercard.sexmod.client.renderer;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages and renders the list of {@link PhysicsParticle} (cummy) particles.
 * Obfuscated name: ga
 */
@OnlyIn(Dist.CLIENT)
public class CummyParticleRenderer {

    static final ResourceLocation TEXTURE =
            new ResourceLocation("sexmod", "textures/cummy.png");

    static final Minecraft MC = Minecraft.getInstance();
    static List<PhysicsParticle> particles = new ArrayList<>();

    @SubscribeEvent
    public void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        RenderSystem.setShaderTexture(0, TEXTURE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        Tesselator   tessellator = Tesselator.getInstance();
        BufferBuilder builder    = tessellator.getBuilder();
        float partialTick = event.getPartialTick();

        try {
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            if (MC.player == null) return;
        } catch (RuntimeException e) {
            return;
        }

        for (PhysicsParticle p : particles) {
            p.render(MC, tessellator, builder, partialTick);
        }

        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        for (PhysicsParticle p : particles) p.tick();
    }

    // -- Static API ------------------------------------------------------------

    public static void add(PhysicsParticle particle) {
        particles.add(particle);
    }

    public static void add(int lifetime, NpcPositionSupplier posSupplier,
                            NpcBoneQuadBuilder quadBuilder, BaseNpcEntity npc,
                            float gravityScale, float size) {
        particles.add(new PhysicsParticle(lifetime, posSupplier, quadBuilder,
                npc, gravityScale, size));
    }

    public static void removeForNpc(@Nonnull BaseNpcEntity npc) {
        List<PhysicsParticle> toRemove = new ArrayList<>();
        for (PhysicsParticle p : particles) {
            if (p.getOwner().getNpcUUID().equals(npc.getNpcUUID())) {
                toRemove.add(p);
            }
        }
        particles.removeAll(toRemove);
    }
}
