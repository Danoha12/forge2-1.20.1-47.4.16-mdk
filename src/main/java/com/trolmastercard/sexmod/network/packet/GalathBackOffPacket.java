package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * GalathBackOffPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Detiene la secuencia de asalto (backOff) de la Galath que pertenece al jugador.
 */
public class GalathBackOffPacket {

    public GalathBackOffPacket() {
        // Paquete sin datos (Empty Payload)
    }

    // ── Serialización ─────────────────────────────────────────────────────────

    public static void encode(GalathBackOffPacket msg, FriendlyByteBuf buf) {
        // No se requiere enviar datos extra
    }

    public static GalathBackOffPacket decode(FriendlyByteBuf buf) {
        return new GalathBackOffPacket();
    }

    // ── Manejador (Handler) ───────────────────────────────────────────────────

    public static void handle(GalathBackOffPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Solo procesamos este paquete si llega al Servidor
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) {
                    System.out.println("[SexMod] Error: Recibido GalathBackOffPacket sin remitente.");
                    return;
                }

                // Buscamos a la Galath que pertenece a este jugador
                // Usamos el método de búsqueda que definimos en BaseNpcEntity
                BaseNpcEntity npc = BaseNpcEntity.getNpcByOwner(player.getUUID(), true);

                if (npc instanceof GalathEntity galath) {
                    // Ejecutamos la lógica de retroceso
                    galath.backOff();
                }
            });
        }

        ctx.setPacketHandled(true);
    }
}