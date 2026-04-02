package com.trolmastercard.sexmod.client.event;

import com.trolmastercard.sexmod.client.gui.ConsentScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "sexmod", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ConsentHandler {

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        // Si el juego intenta abrir el Menú Principal y no hemos aceptado el consentimiento...
        if (event.getScreen() instanceof TitleScreen && !ConsentScreen.hasConsented) {
            // ...Cambiamos la pantalla por nuestra advertencia
            event.setNewScreen(new ConsentScreen(event.getScreen()));
        }
    }
}