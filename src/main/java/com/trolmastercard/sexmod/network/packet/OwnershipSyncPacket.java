package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.data.GalathOwnershipData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * OwnershipSyncPacket — Portado a 1.20.1.
 * * SERVIDOR -> CLIENTE.
 * * Sincroniza el estado de posesión de Galath para la moneda y la UI.
 */
public class OwnershipSyncPacket {

    public final boolean isOwner;

    public OwnershipSyncPacket(boolean isOwner) {
        this.isOwner = isOwner;
    }

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(OwnershipSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isOwner);
    }

    public static OwnershipSyncPacket decode(FriendlyByteBuf buf) {
        return new OwnershipSyncPacket(buf.readBoolean());
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(OwnershipSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Ejecutamos en el hilo principal del cliente
        ctx.enqueueWork(() -> {
            // Usamos DistExecutor para evitar que el servidor busque clases de cliente
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // REPARACIÓN: Accedemos a msg.isOwner directamente
                GalathOwnershipData.clientHasGalath = msg.isOwner;

                // Opcional: Log para debuggear en la consola del cliente
                // System.out.println("[SexMod] Sincronización de Galath: " + msg.isOwner);
            });
        });

        ctx.setPacketHandled(true);
    }
}