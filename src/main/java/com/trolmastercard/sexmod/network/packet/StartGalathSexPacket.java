package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathSexEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * StartGalathSexPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Instruye a todas las instancias de GalathSexEntity que pertenecen a este jugador
 * para que inicien su secuencia de acercamiento.
 */
public class StartGalathSexPacket {

    private final UUID masterUUID;

    public StartGalathSexPacket(UUID masterUUID) {
        this.masterUUID = masterUUID;
    }

    // ── Codec (Optimizado con UUID nativo) ───────────────────────────────────

    public static void encode(StartGalathSexPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.masterUUID);
    }

    public static StartGalathSexPacket decode(FriendlyByteBuf buf) {
        return new StartGalathSexPacket(buf.readUUID());
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(StartGalathSexPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Solo procesar en el lado del servidor
        if (!ctx.getDirection().getReceptionSide().isServer()) return;

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // 1. Verificación de Seguridad (Anti-Cheat)
            // Evitamos que un jugador envíe un paquete falso con el UUID de OTRO jugador
            // para activar a sus NPCs a distancia.
            if (!sender.getUUID().equals(msg.masterUUID)) {
                System.err.println("[SexMod] Seguridad: " + sender.getName().getString() +
                        " intentó iniciar una secuencia de Galath ajena.");
                return;
            }

            // 2. Ejecutar la acción
            // ctx.enqueueWork ya se ejecuta en el hilo principal del servidor,
            // así que no necesitamos ServerLifecycleHooks.execute()
            List<BaseNpcEntity> npcs = BaseNpcEntity.getAllWithMaster(msg.masterUUID);

            for (BaseNpcEntity npc : npcs) {
                // Al estar en el servidor y buscar en getAllWithMaster, ya sabemos que no es ClientSide
                if (npc instanceof GalathSexEntity galathSex && npc.isAlive() && !npc.isRemoved()) {
                    galathSex.startSexApproach(); // fg.a() original
                }
            }
        });

        ctx.setPacketHandled(true);
    }
}