package com.trolmastercard.sexmod.client.event;

import com.trolmastercard.sexmod.PlayerKoboldEntity;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * MenuClearHandler - ported from 1.12.2 to 1.20.1.
 *
 * Limpia las listas de Cliente de PlayerKoboldEntity cuando se abre
 * el menú principal o la pantalla de multijugador, evitando que
 * se queden referencias "fantasma" entre sesiones.
 */
// Reemplaza "sexmod" por tu MOD_ID real si es diferente en tu clase Main
@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MenuClearHandler {

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen() instanceof TitleScreen || event.getScreen() instanceof JoinMultiplayerScreen) {
            // Llama al nuevo método estático que debes añadir a PlayerKoboldEntity
            PlayerKoboldEntity.clearAll();
        }
    }
}