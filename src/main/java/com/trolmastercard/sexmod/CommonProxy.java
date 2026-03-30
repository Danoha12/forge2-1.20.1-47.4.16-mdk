package com.trolmastercard.sexmod;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.registry.ModItems; // Asegúrate de que usen DeferredRegister
import com.trolmastercard.sexmod.registry.ModSounds;
import com.trolmastercard.sexmod.util.ModConstants;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.IOException;

/**
 * CommonProxy — Portado a 1.20.1.
 * * Lógica compartida entre Cliente y Servidor.
 * * NOTA: La registración de Items/Entidades ahora se hace preferiblemente
 * * vía DeferredRegister en sus respectivas clases de registro.
 */
public class CommonProxy {

    /**
     * Reemplaza al antiguo PreInit.
     * * Aquí registramos cosas que no usan el bus de eventos de registro de Forge.
     */
    public void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Inicializar Red (Paquetes)
            ModNetwork.init();

            // Configuración de mundo (WorldGen moderno se hace vía JSON/Datapacks mayormente)
            // WorldGenerationManager.register();

            // Registrar eventos comunes (IA, Interacciones)
            EventRegistrar.register(false);
        });
    }

    /**
     * Reemplaza al antiguo Init.
     */
    public void init(FMLCommonSetupEvent event) {
        try {
            Main.setConfigs();
        } catch (Exception e) {
            System.err.println("[SexMod] Error al cargar configuraciones: " + e.getMessage());
        }
    }

    /**
     * Reemplaza al antiguo PostInit.
     */
    public void loadComplete(FMLLoadCompleteEvent event) {
        setupCustomModelsOnServer();
    }

    /**
     * Carga los modelos personalizados en el servidor.
     * * Evita cargar lógica de renderizado si es un cliente integrado (Singleplayer).
     */
    protected void setupCustomModelsOnServer() {
        var server = ServerLifecycleHooks.getCurrentServer();

        // Si no hay servidor o es el cliente integrado, el cliente ya cargó sus modelos.
        if (server == null || !server.isDedicatedServer()) {
            return;
        }

        System.out.println("[SexMod] Cargando modelos personalizados en el Servidor Dedicado...");
        // CustomModelManager.loadServer(false);
    }
}