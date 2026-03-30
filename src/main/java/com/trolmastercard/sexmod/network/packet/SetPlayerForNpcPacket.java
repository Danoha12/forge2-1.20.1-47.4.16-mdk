package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity; // Asumo que existe este paquete
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SetPlayerForNpcPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Asigna un jugador como compañero/objetivo de todos los NPCs bajo un masterUUID.
 * * Si el NPC tiene la capacidad (KoboldSexEntity), activa la secuencia de acercamiento.
 */
public class SetPlayerForNpcPacket {

    public final UUID masterUUID;
    public final UUID playerUUID;

    // ── Constructores ────────────────────────────────────────────────────────

    public SetPlayerForNpcPacket(UUID masterUUID, UUID playerUUID) {
        this.masterUUID = masterUUID;
        this.playerUUID = playerUUID;
    }

    // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

    public static void encode(SetPlayerForNpcPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.masterUUID); // Mucho más rápido que String
        buf.writeUUID(msg.playerUUID);
    }

    public static SetPlayerForNpcPacket decode(FriendlyByteBuf buf) {
        return new SetPlayerForNpcPacket(buf.readUUID(), buf.readUUID());
    }

    // ── Manejador (Servidor) ─────────────────────────────────────────────────

    public static void handle(SetPlayerForNpcPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // enqueueWork ya es seguro para el hilo principal. No necesitamos otro .execute()
        ctx.enqueueWork(() -> {
            PlayerList playerList = ServerLifecycleHooks.getCurrentServer().getPlayerList();

            // Verificar si el jugador existe en el servidor
            ServerPlayer target = playerList.getPlayer(msg.playerUUID);
            if (target == null) {
                System.out.println("[SexMod] Error: No se pudo encontrar al jugador con UUID: " + msg.playerUUID);
                System.out.println("Jugadores disponibles:");
                for (ServerPlayer p : playerList.getPlayers()) {
                    System.out.println(" - " + p.getName().getString() + " (" + p.getUUID() + ")");
                }
                return;
            }

            // Buscar todos los NPCs vinculados y asignarles el objetivo
            List<BaseNpcEntity> npcs = BaseNpcEntity.getAllWithMaster(msg.masterUUID);
            for (BaseNpcEntity npc : npcs) {

                // Si es un NPC capaz de esta interacción, iniciar la fase de acercamiento
                if (npc instanceof KoboldSexEntity sexNpc) {
                    sexNpc.setApproachEnabled(true);
                }

                npc.setSexPartnerUUID(msg.playerUUID);
            }
        });

        ctx.setPacketHandled(true);
    }
}