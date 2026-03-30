package com.trolmastercard.sexmod.event;

import com.trolmastercard.sexmod.client.gui.NpcCustomizeScreen;
import com.trolmastercard.sexmod.client.model.CustomModelManager;
import com.trolmastercard.sexmod.data.CustomModelSavedData;
import com.trolmastercard.sexmod.item.AlliesLampItem;
import com.trolmastercard.sexmod.item.WinchesterItem;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.util.DevToolsHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;

/**
 * EventRegistrar — Portado a 1.20.1.
 * * Central para registrar manejadores de eventos (Event Handlers) de instancia.
 * * NOTA: Las clases con @Mod.EventBusSubscriber NO deben registrarse aquí para evitar duplicados.
 */
public class EventRegistrar {

    /**
     * Registra todos los manejadores en el bus de Forge.
     * Ya no necesitamos pasar el boolean 'isClient', FMLEnvironment lo sabe de forma nativa.
     */
    public static void register() {
        // ── Handlers de Servidor / Comunes (Solo de Instancia) ──

        // MinecraftForge.EVENT_BUS.register(new NpcDamageHandler());
        // MinecraftForge.EVENT_BUS.register(new PlayerConnectionHandler());
        MinecraftForge.EVENT_BUS.register(new AlliesLampItem.InteractHandler());
        MinecraftForge.EVENT_BUS.register(new TribeManager.WorldEventHandler("tribes"));
        // MinecraftForge.EVENT_BUS.register(new NpcRenderEventHandler());

        // DevToolsHandler usa métodos de instancia, así que sí va aquí
        MinecraftForge.EVENT_BUS.register(new DevToolsHandler());

        // MinecraftForge.EVENT_BUS.register(new GalathOwnershipData());
        // MinecraftForge.EVENT_BUS.register(new CustomModelSavedData());

        // Si el Item en sí maneja eventos y es un Singleton
        // MinecraftForge.EVENT_BUS.register(WinchesterItem.INSTANCE);

        // ── Handlers exclusivos del Cliente ──
        if (FMLEnvironment.dist.isClient()) {
            registerClientHandlers();
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void registerClientHandlers() {
        if (shouldShowConsent()) {
            // Registrar UI de consentimiento (antigua clase fr) cuando la portes
            // MinecraftForge.EVENT_BUS.register(new ConsentScreenHandler());
        }

        // MinecraftForge.EVENT_BUS.register(new MenuClearHandler());
        // MinecraftForge.EVENT_BUS.register(new SexProposalManager());
        MinecraftForge.EVENT_BUS.register(new NpcCustomizeScreen.EventHandler());
        MinecraftForge.EVENT_BUS.register(new CustomModelManager.ClientEventHandler());
    }

    /** * Retorna true si el archivo "dontAskAgain" NO existe.
     * Usamos FMLPaths para garantizar compatibilidad entre OS y Servidores.
     */
    private static boolean shouldShowConsent() {
        File file = FMLPaths.GAMEDIR.get().resolve("sexmod").resolve("dontAskAgain").toFile();
        file.getParentFile().mkdirs();
        return !file.exists();
    }
}