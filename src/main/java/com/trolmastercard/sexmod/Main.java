package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.KoboldEntity;
import com.trolmastercard.sexmod.PlayerKoboldEntity;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.bernie.geckolib.GeckoLib;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main - ported from Main.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Entry point for the Fapcraft mod. Initialises GeckoLib, registers all content,
 * sets up the network channel, and handles world start/stop cleanup.
 *
 * 1.12.2 - 1.20.1:
 *  - @Mod(modid, name, version, dependencies) - @Mod(MODID)
 *  - @Instance / @SidedProxy removed; proxy calls replaced by separate event handlers
 *  - FMLPreInitializationEvent - FMLCommonSetupEvent (modBus)
 *  - FMLInitializationEvent - FMLCommonSetupEvent (modBus)
 *  - FMLServerStartingEvent - ServerStartingEvent (forgeBus)
 *  - FMLServerStoppedEvent - ServerStoppedEvent (forgeBus)
 *  - FMLCommonHandler.instance().getSide() - DistExecutor
 *  - ICommand - Commands.literal (brigadier)
 *  - em.ad() - BaseNpcEntity.getAllNpcs()
 *  - ax.a() - TribeManager.reset()
 *  - ff.aY - KoboldEntity.spawnedKobolds
 *  - v.a() - CustomNamesData.reset()
 *  - g3.b().a() - WorldSettingsManager.get().reset()
 *  - fs.a() - NpcSexStateManager.reset()
 *  - br.e - NpcEditor.enabled
 *  - bj.a() - NpcSkinLoader.reset()
 *  - gm.a() - WorldTreeManager.reset()
 *  - d6.c() - NpcBodyRenderer.preloadAll()
 *  - GeckoLib.initialize() stays - GeckoLib4 init unchanged
 *
 * Config keys:
 *  - shouldGenBuildings - WorldSettingsManager.shouldGenBuildings
 *  - shouldLoadOtherSkins - KoboldSkinLoader.shouldLoadOtherSkins
 *  - allowFlying - PlayerKoboldEntity.allowFlying
 */
@Mod(Main.MODID)
public class Main {

    public static final String MODID   = "sexmod";
    public static final Logger LOGGER  = LogManager.getLogger(MODID);

    private static final String CONFIG_PATH    = "config/sexmod.json";
    private static final String DEFAULT_CONFIG = "{\"shouldGenBuildings\":true,\"shouldLoadOtherSkins\":false,\"allowFlying\":true}";

    public Main() {
        GeckoLib.initialize();
        IEventBus modBus   = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // Register mod lifecycle
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        // Register Forge events
        forgeBus.addListener(this::onServerStarting);
        forgeBus.addListener(this::onServerStopped);
        forgeBus.register(new NpcDeathHandler());
        forgeBus.register(new PlayerCamEventHandler());

        // Defer content registration
        ModEntities.ENTITIES.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);

        try {
            setConfigs();
        } catch (IOException e) {
            LOGGER.error("Failed to load sexmod config", e);
        }
    }

    // -- Lifecycle -------------------------------------------------------------

    private void commonSetup(FMLCommonSetupEvent event) {
        ModNetwork.register();
        CommonSetup.init();
    }

    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ClientSetup.init();
            // Preload goblin / NPC renderer caches
            GoblinEntityRenderer.preloadAll();
        });
    }

    // -- Server events ---------------------------------------------------------

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        event.getServer().getCommands().getDispatcher()
                .register(NpcSpawnCommand.register());
        event.getServer().getCommands().getDispatcher()
                .register(NpcDebugCommand.register());
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        BaseNpcEntity.getAllNpcs().clear();
        TribeManager.reset();
        KoboldEntity.spawnedKobolds.clear();
        CustomNamesData.reset();
        WorldSettingsManager.get().reset();
        NpcSexStateManager.reset();
        NpcEditor.enabled = false;
        NpcSkinLoader.reset();

        net.minecraftforge.fml.util.thread.EffectiveSide.get().isClient();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            clientReset();
        }
    }

    private static void clientReset() {
        WorldTreeManager.reset();
        NpcBodyRenderer.preloadAll();
    }

    // -- Config ----------------------------------------------------------------

    public static void setConfigs() throws IOException {
        Path cfgPath = Paths.get(CONFIG_PATH);
        cfgPath.getParent().toFile().mkdirs();

        if (!cfgPath.toFile().exists()) {
            Files.writeString(cfgPath, DEFAULT_CONFIG);
        }

        String json = Files.readString(cfgPath);

        if (!json.contains("shouldGenBuildings")) {
            // Malformed or outdated - reset to defaults
            Files.writeString(cfgPath, DEFAULT_CONFIG);
            WorldSettingsManager.shouldGenBuildings    = true;
            KoboldSkinLoader.shouldLoadOtherSkins      = false;
            PlayerKoboldEntity.allowFlying             = true;
            return;
        }

        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            WorldSettingsManager.shouldGenBuildings = obj.get("shouldGenBuildings").getAsBoolean();
            KoboldSkinLoader.shouldLoadOtherSkins   = obj.get("shouldLoadOtherSkins").getAsBoolean();
            PlayerKoboldEntity.allowFlying          = obj.get("allowFlying").getAsBoolean();
        } catch (Exception e) {
            LOGGER.warn("Malformed sexmod config, using defaults: {}", e.getMessage());
            WorldSettingsManager.shouldGenBuildings = true;
            KoboldSkinLoader.shouldLoadOtherSkins   = false;
            PlayerKoboldEntity.allowFlying          = true;
        }
    }
}
