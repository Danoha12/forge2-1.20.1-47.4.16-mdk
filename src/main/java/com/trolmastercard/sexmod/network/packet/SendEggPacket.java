package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.registry.ModItems;
import com.trolmastercard.sexmod.util.EyeAndKoboldColor;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * SendEggPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Solicita la generación de un Huevo de Tribu para el jugador actual.
 */
public class SendEggPacket {

    // Paquete vacío, no necesitamos variables

    public SendEggPacket() {}

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(SendEggPacket msg, FriendlyByteBuf buf) {
        // Nada que escribir
    }

    public static SendEggPacket decode(FriendlyByteBuf buf) {
        return new SendEggPacket();
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(SendEggPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // 1. Obtener la tribu del jugador
            UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
            if (tribeId == null) return;

            // 2. Obtener el color de la tribu
            EyeAndKoboldColor color = TribeManager.getTribeColor(tribeId);
            if (color == null) return; // Seguridad extra

            // 3. Construir el ItemStack del Huevo
            ItemStack egg = new ItemStack(ModItems.TRIBE_EGG.get(), 1);

            // 🚨 1.20.1: Uso de putUUID en lugar de putString para mayor rendimiento
            egg.getOrCreateTag().putUUID("tribeID", tribeId);
            egg.getOrCreateTag().putInt("woolMeta", color.getWoolMeta());

            // 4. Dárselo al jugador de forma segura (si está lleno, lo tira al piso)
            if (!player.getInventory().add(egg)) {
                player.drop(egg, false);
            }
        });
        ctx.setPacketHandled(true);
    }
}