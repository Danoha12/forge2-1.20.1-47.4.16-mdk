package com.trolmastercard.sexmod.util; // Ajusta a tu paquete de utilidades

import com.trolmastercard.sexmod.Main; // Asegúrate de tener tu clase Main para el LOGGER
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * ThreadUtil — Portado a 1.20.1.
 * * Determina si el código se está ejecutando en el hilo del servidor o del cliente.
 * * 🚨 OPTIMIZADO: Reemplazada la frágil heurística de Strings por el sistema nativo de Forge.
 */
public class ThreadUtil {

    /**
     * Devuelve true si el hilo actual pertenece al Servidor Lógico.
     */
    public static boolean isServerThread() {
        // 🛡️ Forge 1.20.1: EffectiveSide.get() lee el ThreadGroup y determina el lado lógico real.
        // Adiós a adivinar leyendo el nombre del hilo.
        try {
            return EffectiveSide.get().isServer();
        } catch (Exception e) {
            // Fallback extremo en caso de que Forge no pueda determinar el lado
            // (por ejemplo, en hilos de trabajadores puros antes de la inicialización)
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                boolean onServer = server.isSameThread();
                Main.LOGGER.warn("[SexMod] EffectiveSide falló. Fallback manual a ServerLifecycleHooks. onServer=" + onServer);
                return onServer;
            }
            return false;
        }
    }
}