package com.trolmastercard.sexmod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Event handler that fires a one-shot initialization on the first client tick.
 * In 1.12.2 this was a JFrame subclass; in 1.20.1 it is a plain event listener.
 * Obfuscated name: fr
 */
@OnlyIn(Dist.CLIENT)
public class StartupInitFrame {

    private boolean initialized = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (this.initialized) return;
        this.initialized = true;
        VersionChecker.checkVersion();
    }
}
