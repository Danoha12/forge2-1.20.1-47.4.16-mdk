package com.trolmastercard.sexmod.client.renderer;

import com.trolmastercard.sexmod.client.model.entity.*;
import com.trolmastercard.sexmod.client.renderer.entity.*;
import com.trolmastercard.sexmod.registry.ModEntityRegistry; // O ModEntities según tu proyecto
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * EntityRenderRegistry — Portado a 1.20.1.
 * * Vincula cada EntityType con su respectivo Renderizador y Modelo Geo.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ModConstants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EntityRenderRegistry {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

        // ── NPCs Principales (Chicas) ─────────────────────────────────────────

        event.registerEntityRenderer(ModEntityRegistry.KOBOLD.get(),
                ctx -> new KoboldEntityRenderer(ctx, new KoboldModel(), 0.4F));

        event.registerEntityRenderer(ModEntityRegistry.JENNY.get(),
                ctx -> new NpcSubtypeRenderer(ctx, new JennyModel(), 0.15F));

        event.registerEntityRenderer(ModEntityRegistry.ELLIE.get(),
                ctx -> new NpcBodyRendererAlt(ctx, new EllieModel(), 0.4F));

        event.registerEntityRenderer(ModEntityRegistry.ALLIE.get(),
                ctx -> new AllieRenderer(ctx, new AllieModel(), 0.4F));

        event.registerEntityRenderer(ModEntityRegistry.LUNA.get(),
                ctx -> new LunaRenderer(ctx, new LunaModel(), 0.4F));

        event.registerEntityRenderer(ModEntityRegistry.MANGLELIE.get(),
                ctx -> new MangleLieRenderer(ctx, new MangleLieModel(), 0.4F));

        event.registerEntityRenderer(ModEntityRegistry.GOBLIN.get(),
                ctx -> new NpcColoredRenderer(ctx, new GoblinModel(), 0.6F));

        event.registerEntityRenderer(ModEntityRegistry.GALATH.get(),
                ctx -> new GalathRenderer(ctx, new GalathModel(), 0.05F));

        event.registerEntityRenderer(ModEntityRegistry.SLIME_NPC.get(),
                ctx -> new FigureNpcRenderer(ctx, new SlimeModel(), 0.2F));

        // ── Avatares de Jugador (Morphs) ───────────────────────────────────────

        event.registerEntityRenderer(ModEntityRegistry.PLAYER_KOBOLD.get(),
                ctx -> new PlayerKoboldRenderer(ctx, new KoboldModel(), 0.05F));

        // ── Renderizadores de Cuerpo (Staging / Cinemáticas) ──────────────────

        event.registerEntityRenderer(ModEntityRegistry.JENNY_BODY.get(),
                ctx -> new JennyBodyRenderer(ctx, new JennyModel()));

        // ── Proyectiles y Objetos Técnicos ────────────────────────────────────

        event.registerEntityRenderer(ModEntityRegistry.ENERGY_BALL.get(),
                EnergyBallRenderer::new);

        event.registerEntityRenderer(ModEntityRegistry.WANDERING_ENEMY.get(),
                WanderingEnemyRenderer::new);

        // Si tienes proyectiles personalizados como el de Luna:
        // event.registerEntityRenderer(ModEntityRegistry.LUNA_HOOK.get(),
        //         ctx -> new FishingLineRenderer(ctx));
    }
}