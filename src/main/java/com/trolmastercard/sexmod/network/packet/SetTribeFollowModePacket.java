package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.tribe.TribeManager; // Asumo que existe en este paquete
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * SetTribeFollowModePacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Alterna el modo de seguimiento (followMode) para la tribu del jugador que envía el paquete.
 */
public class SetTribeFollowModePacket {

    public final boolean followMode;

    // ── Constructor ──────────────────────────────────────────────────────────

    public SetTribeFollowModePacket(boolean followMode) {
        this.followMode = followMode;
    }

    // ── Codec (Estandarizado 1.20.1) ─────────────────────────────────────────

    public static void encode(SetTribeFollowModePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.followMode);
    }

    public static SetTribeFollowModePacket decode(FriendlyByteBuf buf) {
        return new SetTribeFollowModePacket(buf.readBoolean());
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(SetTribeFollowModePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) {
                System.out.println("[SexMod] Error: Se recibió un @SetTribeFollowMode sin jugador remitente.");
                return;
            }

            // Buscar el ID de la tribu a la que pertenece este jugador
            UUID tribeUUID = TribeManager.getTribeUUIDForPlayer(sender.getUUID());
            if (tribeUUID == null) return; // El jugador no tiene tribu

            // Aplicar el cambio de modo
            TribeManager.setFollowMode(tribeUUID, msg.followMode);
        });

        ctx.setPacketHandled(true);
    }
}