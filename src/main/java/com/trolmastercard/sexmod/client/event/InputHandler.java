package com.trolmastercard.sexmod.client.event;

import com.trolmastercard.sexmod.client.ModKeyBindings;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "sexmod", value = Dist.CLIENT)
public class InputHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 1. Detectar tecla de Acción (R)
        while (ModKeyBindings.ACTION_KEY.consumeClick()) {
            // Aquí enviarías un paquete al servidor:
            // PacketHandler.sendToServer(new ActionPacket("toggle_pose"));
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[SexMod] §fAcción activada"), true);
        }

        // 2. Detectar Vuelo (Flechas)
        if (ModKeyBindings.FLY_UP.isDown()) {
            // Lógica para subir la cámara o el NPC
            // PacketHandler.sendToServer(new FlightPacket(0.1f));
        }

        if (ModKeyBindings.FLY_DOWN.isDown()) {
            // Lógica para bajar
            // PacketHandler.sendToServer(new FlightPacket(-0.1f));
        }
    }
}