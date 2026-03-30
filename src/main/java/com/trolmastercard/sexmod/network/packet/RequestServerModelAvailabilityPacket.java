package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.client.CustomModelManager; // Ajusta a tu paquete real
import com.trolmastercard.sexmod.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * RequestServerModelAvailabilityPacket — Portado a 1.20.1.
 * * Paquete Bidireccional de negociación de modelos:
 * * C -> S: (Mapa vacío) El cliente pide la lista de modelos.
 * * S -> C: (Mapa lleno) El servidor envía su catálogo {nombre -> versión}.
 */
public class RequestServerModelAvailabilityPacket {

    public final HashMap<String, Float> models;

    // ── Constructores ────────────────────────────────────────────────────────

    /** Constructor para C -> S (Petición inicial vacía) */
    public RequestServerModelAvailabilityPacket() {
        this.models = new HashMap<>();
    }

    /** Constructor para S -> C (Respuesta del servidor con el catálogo) */
    public RequestServerModelAvailabilityPacket(HashMap<String, Float> models) {
        this.models = models != null ? models : new HashMap<>();
    }

    // ── Codec (Agnóstico, SIN proxies) ───────────────────────────────────────

    public static void encode(RequestServerModelAvailabilityPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.models.size());
        for (Map.Entry<String, Float> entry : msg.models.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeFloat(entry.getValue());
        }
    }

    public static RequestServerModelAvailabilityPacket decode(FriendlyByteBuf buf) {
        HashMap<String, Float> map = new HashMap<>();
        try {
            int count = buf.readInt();
            for (int i = 0; i < count; i++) {
                map.put(buf.readUtf(), buf.readFloat());
            }
        } catch (Exception e) {
            System.err.println("[SexMod] Error decodificando disponibilidad de modelos: " + e.getMessage());
        }
        return new RequestServerModelAvailabilityPacket(map);
    }

    // ── Manejador Bidireccional ──────────────────────────────────────────────

    public static void handle(RequestServerModelAvailabilityPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            // 1. LÓGICA DEL CLIENTE (Recibimos el catálogo del servidor)
            ctx.enqueueWork(() -> {
                // 🛡️ Aislamos la ejecución del cliente para proteger al servidor dedicado
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClientSide(msg.models));
            });
        } else {
            // 2. LÓGICA DEL SERVIDOR (Recibimos la petición del cliente)
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    // Le enviamos nuestro catálogo de versiones
                    ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> sender),
                            new RequestServerModelAvailabilityPacket(CustomModelManager.getServerModelVersions())
                    );
                }
            });
        }
        ctx.setPacketHandled(true);
    }

    // ── Aislamiento del Cliente ──────────────────────────────────────────────

    private static void handleClientSide(HashMap<String, Float> serverModels) {
        if (!CustomModelManager.isEnabled()) return;

        List<String> needed = new ArrayList<>();

        for (Map.Entry<String, Float> entry : serverModels.entrySet()) {
            String name = entry.getKey();
            float serverVer = entry.getValue();

            if (!CustomModelManager.hasModel(name)) {
                needed.add(name); // No lo tenemos
            } else {
                float clientVer = CustomModelManager.getModelVersion(name);
                if (serverVer > clientVer) {
                    needed.add(name); // Tenemos una versión vieja
                }
            }
        }

        // Respondemos al servidor con la lista de lo que nos falta
        if (!needed.isEmpty()) {
            ModNetwork.CHANNEL.sendToServer(new SyncCustomModelsPacket(needed));
        }
    }
}