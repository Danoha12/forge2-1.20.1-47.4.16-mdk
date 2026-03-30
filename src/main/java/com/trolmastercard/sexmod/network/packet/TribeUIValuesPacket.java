package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;
import java.util.function.Supplier;

/**
 * TribeUIValuesPacket — Portado a 1.20.1.
 * * Paquete BIDIRECCIONAL para actualizar el minimapa de la tribu.
 * * CLIENTE -> SERVIDOR: Paquete vacío (petición).
 * * SERVIDOR -> CLIENTE: Paquete con coordenadas y colores.
 */
public class TribeUIValuesPacket {

    public final boolean hasTribe;
    public final List<double[]> positions; // Cada uno: [x, y, z, colorIndex]

    // ── Constructores ────────────────────────────────────────────────────────

    public TribeUIValuesPacket(boolean hasTribe, List<double[]> positions) {
        this.hasTribe = hasTribe;
        this.positions = positions;
    }

    /** Petición vacía (CLIENTE → SERVIDOR) */
    public static TribeUIValuesPacket request() {
        return new TribeUIValuesPacket(false, new ArrayList<>());
    }

    // ── Codec (Corregido a Double) ───────────────────────────────────────────

    public static void encode(TribeUIValuesPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasTribe);
        buf.writeInt(msg.positions.size());
        for (double[] p : msg.positions) {
            buf.writeDouble(p[0]); // X
            buf.writeDouble(p[1]); // Y
            buf.writeDouble(p[2]); // Z
            buf.writeDouble(p[3]); // Color (como double para mantener la firma del array)
        }
    }

    public static TribeUIValuesPacket decode(FriendlyByteBuf buf) {
        boolean hasTribe = buf.readBoolean();
        int count = buf.readInt();
        List<double[]> positions = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            positions.add(new double[]{
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()
            });
        }
        return new TribeUIValuesPacket(hasTribe, positions);
    }

    // ── Manejador Principal ──────────────────────────────────────────────────

    public static void handle(TribeUIValuesPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                // 🛡️ Aislamiento total del cliente para evitar crasheos en servidores dedicados
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg));
            } else {
                // Ejecución en el servidor
                handleServer(msg, ctx.getSender());
            }
        });

        ctx.setPacketHandled(true);
    }

    // ── Lógica del Servidor (Responde al Cliente) ────────────────────────────

    private static void handleServer(TribeUIValuesPacket msg, ServerPlayer sender) {
        if (sender == null) return;

        UUID playerUUID = sender.getUUID();
        UUID tribeId = TribeManager.getTribeIdForMaster(playerUUID);

        // Si no tiene tribu, respondemos con el paquete vacío
        if (tribeId == null) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), request());
            return;
        }

        boolean hasTribeResult = TribeManager.isActive(tribeId);
        Map<UUID, BlockPos> savedPositions = TribeManager.getSavedPositions(tribeId, sender.level());
        List<KoboldEntity> loaded = TribeManager.getMembersLoaded(tribeId);

        List<double[]> result = new ArrayList<>();
        int baseColor = TribeManager.getTribeColor(tribeId).getWoolMeta();
        Set<UUID> seen = new HashSet<>();

        // 1. Añadir miembros cargados (en vivo)
        for (KoboldEntity kob : loaded) {
            if (kob.isRemoved()) continue;
            UUID kid = kob.getKoboldUUID();
            if (seen.contains(kid)) continue;

            int color = baseColor;
            if (kob.isLeader()) {
                // Asumo que KoboldColor existe en tu paquete
                color = com.trolmastercard.sexmod.entity.KoboldColor.safeValueOf(kob.getLeaderColorName()).getWoolMeta();
            }
            result.add(new double[]{kob.getX(), kob.getY(), kob.getZ(), color});
            seen.add(kid);
        }

        // 2. Añadir posiciones guardadas (descargadas)
        for (Map.Entry<UUID, BlockPos> entry : savedPositions.entrySet()) {
            if (seen.contains(entry.getKey())) continue;
            BlockPos bp = entry.getValue();
            result.add(new double[]{bp.getX(), bp.getY(), bp.getZ(), baseColor});
        }

        // Enviar respuesta al jugador que la solicitó
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new TribeUIValuesPacket(hasTribeResult, result));
    }

    // ── Lógica del Cliente (Actualiza el HUD) ────────────────────────────────

    private static void handleClient(TribeUIValuesPacket msg) {
        // Asumiendo que TribeScreen tiene acceso estático a hasTribe
        com.trolmastercard.sexmod.client.screen.TribeScreen.hasTribe = msg.hasTribe;
        KoboldEntity.setTribeHighlightPositions(msg.positions);
    }
}