package com.trolmastercard.sexmod.client.event; // Ajusta el paquete

import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * MenuClearHandler — Portado a 1.20.1.
 * * Lado del Cliente.
 * * Limpia las listas estáticas de PlayerKoboldEntity cuando el jugador sale
 * * al menú principal o a la lista de servidores, evitando fugas de memoria y crashes.
 */
// ¡Magia de Forge moderna! Esto registra la clase automáticamente en el bus FORGE del cliente
@Mod.EventBusSubscriber(modid = "sexmod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MenuClearHandler {

    // Al usar EventBusSubscriber, el método DEBE ser estático
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen || event.getScreen() instanceof JoinMultiplayerScreen) {

            // Limpiamos las cachés estáticas para la nueva sesión
            PlayerKoboldEntity.clearAll();

            // System.out.println("[SexMod] Caché de Kobolds limpiada al salir del mundo.");
        }
    }
}