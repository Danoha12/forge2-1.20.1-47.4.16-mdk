package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * NpcActionQueuePacket — Portado a 1.20.1.
 * Maneja la cola de acciones de los NPCs a través de la red.
 */
public class NpcActionQueuePacket {
    private final int entityId;
    private final int actionId;

    public NpcActionQueuePacket(int entityId, int actionId) {
        this.entityId = entityId;
        this.actionId = actionId;
    }

    // Leer los datos del "paquete" que viene por el cable
    public static NpcActionQueuePacket decode(FriendlyByteBuf buf) {
        return new NpcActionQueuePacket(buf.readInt(), buf.readInt());
    }

    // Escribir los datos para mandarlos por el cable
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(actionId);
    }

    // Aquí es donde se ejecuta la lógica cuando llega el mensaje
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Aquí iría la lógica para que el NPC ejecute la acción
            // Por ahora lo dejamos listo para que el compilador no llore
        });
        ctx.get().setPacketHandled(true);
    }
}