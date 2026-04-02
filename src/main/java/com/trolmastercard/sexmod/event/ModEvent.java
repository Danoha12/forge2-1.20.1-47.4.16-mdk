package com.trolmastercard.sexmod.event;

import com.trolmastercard.sexmod.client.ModKeyBindings;
import com.trolmastercard.sexmod.command.NpcSpawnCommand;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "sexmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvent {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Registramos nuestro comando en el despachador de Minecraft
        NpcSpawnCommand.register(event.getDispatcher());
    }
    @SubscribeEvent
    public static void onKeyRegister(RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.ACTION_KEY);
        event.register(ModKeyBindings.FLY_UP);
        event.register(ModKeyBindings.FLY_DOWN);
    }
}