package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * ResetControllerPacket — Portado a 1.20.1.
 * * PAQUETE BIDIRECCIONAL.
 * * Reinicia los contadores de ticks de la animación actual.
 * * El Servidor lo recibe, lo aplica y lo re-transmite a los jugadores cercanos.
 */
public class ResetControllerPacket {

    // 100 bloques de distancia (100 * 100 = 10000 para cálculo optimizado de CPU)
    private static final double BROADCAST_RANGE_SQR = 10000.0;

    private final UUID npcUUID;

    public ResetControllerPacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // ── Codec (Optimizado con UUID nativo) ───────────────────────────────────

    public static void encode(ResetControllerPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUUID);
    }

    public static ResetControllerPacket decode(FriendlyByteBuf buf) {
        return new ResetControllerPacket(buf.readUUID());
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(ResetControllerPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            BaseNpcEntity npc = findNpc(msg.npcUUID);
            if (npc == null || npc.getAnimState() == null) return;

            if (ctx.getDirection().getReceptionSide().isServer()) {

                // ── LADO DEL SERVIDOR ──
                ServerPlayer sender = ctx.getSender();
                if (sender == null) return;

                // Reiniciar contadores en el servidor
                npc.getAnimState().ticksPlaying[0] = 0;
                npc.getAnimState().ticksPlaying[1] = 0;

                // Re-transmitir a los jugadores en un radio de 100 bloques (excepto al remitente)
                for (ServerPlayer player : sender.server.getPlayerList().getPlayers()) {
                    if (player.getUUID().equals(sender.getUUID())) continue;

                    if (player.level() == npc.level() && player.distanceToSqr(npc) < BROADCAST_RANGE_SQR) {
                        ModNetwork.CHANNEL.send(
                                PacketDistributor.PLAYER.with(() -> player),
                                new ResetControllerPacket(msg.npcUUID)
                        );
                    }
                }

            } else {

                // ── LADO DEL CLIENTE ──
                // Reiniciar contadores locales del cliente para que la animación se vea fluida
                npc.getAnimState().ticksPlaying[0] = 0;
                npc.getAnimState().ticksPlaying[1] = 0;

                // Si tienes un método específico resetController() en tu entidad, llámalo aquí:
                // npc.resetController();
            }
        });

        ctx.setPacketHandled(true);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private static BaseNpcEntity findNpc(UUID uuid) {
        for (BaseNpcEntity entity : BaseNpcEntity.getAllActive()) {
            if (entity.getNpcUUID().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }
}