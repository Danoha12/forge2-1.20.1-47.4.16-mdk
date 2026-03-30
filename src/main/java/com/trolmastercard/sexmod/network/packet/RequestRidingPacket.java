package com.trolmastercard.sexmod.network.packet; // Ajusta al paquete de red

import com.trolmastercard.sexmod.data.GalathOwnershipData;
import com.trolmastercard.sexmod.registry.AnimState;
import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * RequestRidingPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Hace que el jugador monte a su NPC vinculado (Galath) y comience el vuelo.
 */
public class RequestRidingPacket {

    // Paquete vacío: no necesitamos variables de estado.

    public RequestRidingPacket() {}

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(RequestRidingPacket msg, FriendlyByteBuf buf) {
        // Nada que escribir
    }

    public static RequestRidingPacket decode(FriendlyByteBuf buf) {
        // Nada que leer
        return new RequestRidingPacket();
    }

    // ── Manejador (Servidor) ─────────────────────────────────────────────────

    public static void handle(RequestRidingPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // 1. Buscar la UUID del NPC vinculado a este jugador
            UUID npcUUID = GalathOwnershipData.getOwnerOf(player.getUUID());
            if (npcUUID == null) return;

            // 2. Buscar la entidad viva en el servidor
            BaseNpcEntity npc = BaseNpcEntity.getByMasterUUID(npcUUID);
            if (npc == null || !npc.isAlive()) return;

            // 3. Montar al NPC (true = forzar montaje)
            player.startRiding(npc, true);

            // 4. Cambiar animación a Vuelo Controlado
            npc.setAnimState(AnimState.CONTROLLED_FLIGHT);

            // 5. Asignar el objetivo / conductor
            npc.setPlayerTarget(player);

            // 6. Impulso inicial hacia arriba (+0.25 Y)
            npc.setDeltaMovement(npc.getDeltaMovement().x, 0.25D, npc.getDeltaMovement().z);

            // 7. Marcar el chunk como modificado para que guarde el estado
            player.level().getChunkAt(npc.blockPosition()).setUnsaved(true);
        });

        ctx.setPacketHandled(true);
    }
}