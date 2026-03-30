package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.LunaEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * CatEatingDonePacket — Portado a 1.20.1.
 * * Enviado por el cliente cuando la animación de comer del gato (Luna) termina.
 * * Le indica al servidor que avance la máquina de estados.
 */
public class CatEatingDonePacket {

    private final UUID npcUUID;

    public CatEatingDonePacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // ── Serialización Optimizada (1.20.1) ────────────────────────────────────

    public static void encode(CatEatingDonePacket msg, FriendlyByteBuf buf) {
        // Enviar UUID nativo ahorra ancho de banda respecto a Strings
        buf.writeUUID(msg.npcUUID);
    }

    public static CatEatingDonePacket decode(FriendlyByteBuf buf) {
        return new CatEatingDonePacket(buf.readUUID());
    }

    // ── Manejador (Handler) ───────────────────────────────────────────────────

    public static void handle(CatEatingDonePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                // LADO SERVIDOR
                ServerPlayer sender = ctx.getSender();
                if (sender == null) return;

                ServerLevel level = sender.serverLevel();

                // Usamos el buscador nativo por UUID del ServerLevel (Súper rápido)
                Entity entity = level.getEntity(msg.npcUUID);

                if (entity instanceof LunaEntity Luna) {
                    Luna.onEatingDone();
                } else {
                    System.out.println("[SexMod] CatEatingDonePacket: Entidad no encontrada o no es un gato.");
                }
            } else {
                // Si por algún motivo llega al cliente, lo ignoramos
                System.out.println("[SexMod] Error: Mensaje CatEatingDone recibido en el cliente :(");
            }
        });

        ctx.setPacketHandled(true);
    }
}