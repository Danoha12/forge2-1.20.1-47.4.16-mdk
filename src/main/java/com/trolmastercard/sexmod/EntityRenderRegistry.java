package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.ModEntityRegistry;

import net.minecraftforge.client.event.EntityRenderersEvent;

/**
 * Registers custom renderers for all mod entities.
 * Called from the client setup event.
 * Obfuscated name: fk
 */
public class EntityRenderRegistry {

    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        // -- Main NPC entities --------------------------------------------------
        event.registerEntityRenderer(ModEntityRegistry.KOBOLD.get(),
                ctx -> new KoboldEntityRenderer(ctx, new KoboldModel(), -0.4D));

        event.registerEntityRenderer(ModEntityRegistry.JENNY.get(),
                ctx -> new NpcSubtypeRenderer(ctx, new JennyModel(), -0.15D));

        event.registerEntityRenderer(ModEntityRegistry.PLAYER_KOBOLD.get(),
                ctx -> new PlayerKoboldRenderer(ctx, new EllieModel(), 0.05D));

        event.registerEntityRenderer(ModEntityRegistry.SLIME_NPC.get(),
                ctx -> new FigureNpcRenderer(ctx, new SlimeModel(), -0.2D));

        event.registerEntityRenderer(ModEntityRegistry.ELLIE.get(),
                ctx -> new NpcBodyRendererAlt(ctx, new BiaModel(), -0.4D));

        event.registerEntityRenderer(ModEntityRegistry.ALLIE.get(),
                ctx -> new AllieRenderer(ctx, new AllieModel(), -0.4D));

        event.registerEntityRenderer(ModEntityRegistry.MANGLE_LIE.get(),
                ctx -> new MangleLieRenderer(ctx, new BeeModel(), -0.4D));

        event.registerEntityRenderer(ModEntityRegistry.KOBOLD_EGG_ENTITY.get(),
                ctx -> new KoboldEggRenderer(ctx, new EggModel()));

        event.registerEntityRenderer(ModEntityRegistry.NPC_INVENTORY.get(),
                ctx -> new NpcInventoryRenderer(ctx, new CatModel(), -0.4D));

        event.registerEntityRenderer(ModEntityRegistry.GOBLIN.get(),
                ctx -> new NpcColoredRenderer(ctx, new GoblinModel(), -0.6D));

        event.registerEntityRenderer(ModEntityRegistry.GALATH.get(),
                ctx -> new GalathRenderer(ctx, new GalathModel(), -0.05D));

        event.registerEntityRenderer(ModEntityRegistry.KOBOLD_EGG_ITEM_ENTITY.get(),
                ctx -> new KoboldEggEntityRenderer(ctx, new KoboldEggItemModel()));

        event.registerEntityRenderer(ModEntityRegistry.MANGLELIE_SEX.get(),
                ctx -> new MangleLieSexRenderer(ctx, new MangleLieModel(), -0.05D));

        // -- Body/hand alt renderers --------------------------------------------
        event.registerEntityRenderer(ModEntityRegistry.ELLIE_BODY.get(),
                ctx -> new NpcBodyRendererAlt(ctx, new BiaModel()));

        event.registerEntityRenderer(ModEntityRegistry.JENNY_BODY.get(),
                ctx -> new JennyBodyRenderer(ctx, new JennyModel()));

        event.registerEntityRenderer(ModEntityRegistry.PLAYER_KOBOLD_BODY.get(),
                ctx -> new NpcBodyRendererAlt(ctx, new EllieModel()));

        event.registerEntityRenderer(ModEntityRegistry.SLIME_BODY.get(),
                ctx -> new SlimeHandRenderer(ctx, new SlimeModel()));

        event.registerEntityRenderer(ModEntityRegistry.ALLIE_BODY.get(),
                ctx -> new AllieBodyRenderer(ctx, new AllieModel()));

        event.registerEntityRenderer(ModEntityRegistry.GOBLIN_BODY.get(),
                ctx -> new GoblinHandRenderer(ctx, new GoblinModel()));

        event.registerEntityRenderer(ModEntityRegistry.MANGLELIE_BODY.get(),
                ctx -> new MangleLieRenderer(ctx, new MangleLieModel()));

        event.registerEntityRenderer(ModEntityRegistry.GALATH_BODY.get(),
                ctx -> new GalathRenderer(ctx, new GalathModel()));

        event.registerEntityRenderer(ModEntityRegistry.KOBOLD_HAND.get(),
                ctx -> new KoboldHandRenderer(ctx, new KoboldModel()));

        event.registerEntityRenderer(ModEntityRegistry.JENNY_HAND.get(),
                ctx -> new JennyHandRenderer(ctx, new JennyModel()));

        // -- Special/utility entities -------------------------------------------
        event.registerEntityRenderer(ModEntityRegistry.FISHING_HOOK.get(),
                ctx -> new FishingLineRenderer(ctx));

        event.registerEntityRenderer(ModEntityRegistry.CLOTHING_OVERLAY.get(),
                ctx -> new NpcBodyRendererAlt(ctx, new ClothingOverlayModel()));

        event.registerEntityRenderer(ModEntityRegistry.ENERGY_BALL.get(),
                ctx -> new EnergyBallRenderer(ctx));

        event.registerEntityRenderer(ModEntityRegistry.WANDERING_ENEMY.get(),
                ctx -> new WanderingEnemyRenderer(ctx));
    }
}
