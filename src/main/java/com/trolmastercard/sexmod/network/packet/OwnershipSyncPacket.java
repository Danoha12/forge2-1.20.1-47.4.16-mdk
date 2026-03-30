package com.trolmastercard.sexmod.network.packet; // Ajusta al paquete correcto

import com.trolmastercard.sexmod.data.GalathOwnershipData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * OwnershipSyncPacket — Portado a 1.20.1.
 * * SERVIDOR -> CLIENTE.
 * * Informa al cliente si el jugador local es el dueño de un NPC (Galath).
 */
public class OwnershipSyncPacket {

    public final boolean isOwner;

    public OwnershipSyncPacket(boolean isOwner) {
        this.isOwner = isOwner;
    }

    // ── Codec (Estandarizado a estático) ─────────────────────────────────────

    public static void encode(OwnershipSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isOwner);
    }

    public static OwnershipSyncPacket decode(FriendlyByteBuf buf) {
        return new OwnershipSyncPacket(buf.readBoolean());
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(OwnershipSyncPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                // 🛡️ ESCUDO DE SERVIDOR DEDICADO:
                // Aísla completamente la ejecución del código de Cliente.
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    GalathOwnershipData.setLocalPlayerIsOwner(msg.isOwner);
                });
            });
        } else {
            System.out.println("[SexMod] Error: OwnershipSyncPacket recibido en el servidor.");
        }

        ctx.setPacketHandled(true);
    }
}