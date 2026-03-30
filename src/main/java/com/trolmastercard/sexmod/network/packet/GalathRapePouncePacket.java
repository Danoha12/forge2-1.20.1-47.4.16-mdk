package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * GalathRapePouncePacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Activa o desactiva el estado de "Pounce" (Abalanzarse) en la GalathEntity
 * que pertenece al jugador que envía el paquete.
 */
public class GalathRapePouncePacket {

    private final boolean pounce;

    public GalathRapePouncePacket(boolean pounce) {
        this.pounce = pounce;
    }

    // ── Serialización (Codec) ─────────────────────────────────────────────────

    public static void encode(GalathRapePouncePacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.pounce);
    }

    public static GalathRapePouncePacket decode(FriendlyByteBuf buf) {
        return new GalathRapePouncePacket(buf.readBoolean());
    }

    // ── Manejador (Handler) ───────────────────────────────────────────────────

    public static void handle(GalathRapePouncePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Verificamos que el paquete sea procesado solo en el Servidor
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) {
                    System.out.println("[SexMod] Error: GalathRapePouncePacket recibido sin remitente.");
                    return;
                }

                // Buscamos a la Galath que pertenece a este jugador (UUID)
                // Nota: Usamos el método de búsqueda estático definido previamente
                BaseNpcEntity npc = BaseNpcEntity.getNpcByOwner(player.getUUID(), true);

                if (npc instanceof GalathEntity galath) {
                    // Actualizamos el estado en la entidad (Lado Servidor)
                    // Asegúrate de que setPounce(boolean) esté en tu clase GalathEntity
                    galath.setPounce(msg.pounce);
                }
            });
        }

        ctx.setPacketHandled(true);
    }

    public boolean isPounce() {
        return pounce;
    }
}