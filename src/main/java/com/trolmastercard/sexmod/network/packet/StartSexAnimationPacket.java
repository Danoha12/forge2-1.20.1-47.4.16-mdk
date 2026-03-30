package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * StartSexAnimationPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Dispara una animación de interacción en un PlayerKoboldEntity.
 * * Optimizado usando lectura/escritura nativa de UUIDs y búsqueda directa en el nivel del servidor.
 */
public class StartSexAnimationPacket {

    private final UUID npcUUID;
    private final UUID playerUUID;
    private final String actionName;

    public StartSexAnimationPacket(UUID npcUUID, UUID playerUUID, String actionName) {
        this.npcUUID = npcUUID;
        this.playerUUID = playerUUID;
        this.actionName = actionName;
    }

    // ── Serialización (Codificar/Decodificar) ────────────────────────────────

    public static void encode(StartSexAnimationPacket msg, FriendlyByteBuf buf) {
        // En 1.20.1 siempre usa writeUUID, es infinitamente más eficiente que mandar Strings
        buf.writeUUID(msg.npcUUID);
        buf.writeUUID(msg.playerUUID);
        buf.writeUtf(msg.actionName);
    }

    public static StartSexAnimationPacket decode(FriendlyByteBuf buf) {
        return new StartSexAnimationPacket(
                buf.readUUID(),
                buf.readUUID(),
                buf.readUtf()
        );
    }

    // ── Manejador del Paquete (Lado del Servidor) ─────────────────────────────

    public static void handle(StartSexAnimationPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Verificamos que este paquete realmente haya llegado al servidor
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            System.out.println("[SexMod] Received StartSexAnimationPacket on wrong side!");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            // Obtenemos al jugador que envió el paquete
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            PlayerKoboldEntity target = null;

            // 1. Búsqueda nativa ultra rápida de la 1.20.1 (Segura para multijugador)
            Entity entity = sender.serverLevel().getEntity(msg.npcUUID);
            if (entity instanceof PlayerKoboldEntity pk) {
                target = pk;
            }
            // 2. Fallback por si la entidad está en un chunk recién cargado o no registrada nativamente
            else {
                target = PlayerKoboldEntity.getByNpcUUID(msg.npcUUID);
            }

            // Ejecutar la acción si encontramos al NPC
            if (target != null) {
                target.onActionSelected(msg.actionName, msg.playerUUID);
            }
        });

        // Marcamos el paquete como procesado para que Forge no lo descarte
        ctx.setPacketHandled(true);
    }
}