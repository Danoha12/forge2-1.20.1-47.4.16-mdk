package com.trolmastercard.sexmod.client.event; // Ajusta a tu paquete de eventos de cliente

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
// import com.trolmastercard.sexmod.util.VersionChecker; // Asegúrate de importar esto

/**
 * StartupInitFrame — Portado a 1.20.1.
 * * Evento "One-shot" que se ejecuta en el primer tick del cliente.
 * * (Y gracias a Notch ya no es una ventana emergente de JFrame 🤣).
 */
// 🚨 1.20.1: Autoregistro en el bus de Forge exclusivo para el Cliente
@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class StartupInitFrame {

    // Cambiado a estático porque los métodos del EventBusSubscriber deben ser estáticos
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // Evitamos que intente ejecutarse dos veces en el mismo tick (START y END)
        if (event.phase != TickEvent.Phase.END) return;

        // Disparo único
        if (!initialized) {
            initialized = true;

            // Llamamos a tu verificador de versión (asumiendo que tiene un método estático checkVersion())
            // VersionChecker.checkVersion();
            System.out.println("[SexMod] Ejecutando inicialización de primer tick (Version Check)...");
        }
    }
}