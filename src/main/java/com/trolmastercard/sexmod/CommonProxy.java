package com.trolmastercard.sexmod;
import com.trolmastercard.sexmod.ModEntityRegistry;

import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;

/**
 * CommonProxy - ported from CommonProxy.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Server-side (and base) proxy. Handles mod registration that must happen
 * on both client and server.
 *
 * 1.12.2 - 1.20.1 migrations:
 *   - FMLPreInitializationEvent - FMLCommonSetupEvent (Forge lifecycle)
 *   - FMLInitializationEvent    - FMLCommonSetupEvent
 *   - FMLPostInitializationEvent - FMLLoadCompleteEvent
 *   - GameRegistry.registerWorldGenerator - handled by WorldGenerationManager
 *   - bi.a() - ModEntityRegistry.register()
 *   - f9.a() - ModItems.register()
 *   - c.a()  - ModSounds.register()
 *   - NetworkRegistry.INSTANCE.registerGuiHandler - MenuScreens.register() (in ClientProxy)
 *   - bn.a(false) - EventRegistrar.register(false)
 *   - ge.a() - ModNetwork.init()
 *   - FMLCommonHandler.instance().getMinecraftServerInstance() - ServerLifecycleHooks.getCurrentServer()
 *   - server.func_71262_S() - server.isDedicatedServer()
 *   - br.c(false) - CustomModelManager.loadServer(false)
 */
public class CommonProxy {

    public void preInit(FMLCommonSetupEvent event) {
        WorldGenerationManager.register();
        ModEntityRegistry.register();
        ModItems.register();
    }

    public void init(FMLCommonSetupEvent event) throws IOException {
        Main.setConfigs();
        ModSounds.register();
        // GUI handler registration is done via MenuScreens in 1.20.1 - see ClientProxy
        EventRegistrar.register(false);
        ModNetwork.init();
    }

    public void postInit(FMLLoadCompleteEvent event) throws IOException {
        setupCustomModelsOnServer();
    }

    protected void setupCustomModelsOnServer() {
        var server = ServerLifecycleHooks.getCurrentServer();
        try {
            if (server != null && !server.isDedicatedServer()) return;
        } catch (RuntimeException e) {
            throw e;
        }
        CustomModelManager.loadServer(false);
    }
}
