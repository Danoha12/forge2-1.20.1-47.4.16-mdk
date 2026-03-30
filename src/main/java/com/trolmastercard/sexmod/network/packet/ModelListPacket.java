package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.client.screen.NpcTypeSelectScreen;
import com.trolmastercard.sexmod.entity.NpcType;
import com.trolmastercard.sexmod.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * ModelListPacket — Portado a 1.20.1.
 * * BIDIRECCIONAL:
 * - CLIENTE -> SERVIDOR (Vacío): Solicita los overrides del NBT del jugador.
 * - SERVIDOR -> CLIENTE (Payload): Envía el mapa de overrides y abre la GUI de selección.
 */
public class ModelListPacket {

    private final Map<NpcType, String> overrides;

    /** Constructor para el servidor (enviar datos al cliente). */
    public ModelListPacket(Map<NpcType, String> overrides) {
        this.overrides = overrides;
    }

    /** Constructor para el cliente (solicitar datos). */
    public static ModelListPacket request() {
        return new ModelListPacket(new HashMap<>());
    }

    // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

    public static void encode(ModelListPacket msg, FriendlyByteBuf buf) {
        // FriendlyByteBuf tiene un método nativo para escribir mapas, ¡usémoslo!
        buf.writeMap(msg.overrides,
                (b, type) -> b.writeEnum(type), // Escribe el Enum de forma eficiente
                (b, model) -> b.writeUtf(model)
        );
    }

    public static ModelListPacket decode(FriendlyByteBuf buf) {
        // Leemos el mapa de vuelta con la misma lógica
        return new ModelListPacket(buf.readMap(
                b -> b.readEnum(NpcType.class),
                FriendlyByteBuf::readUtf
        ));
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(ModelListPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // Verificamos si estamos en el Servidor o en el Cliente
            if (ctx.getDirection().getReceptionSide().isServer()) {
                handleServerSide(ctx.getSender());
            } else {
                handleClientSide(msg);
            }
        });
        ctx.setPacketHandled(true);
    }

    /** Lógica en el Servidor: Lee el NBT y responde al cliente. */
    private static void handleServerSide(ServerPlayer sender) {
        if (sender == null) return;

        Map<NpcType, String> result = new HashMap<>();
        for (NpcType type : NpcType.values()) {
            if (!type.hasSpecifics) continue;

            // Leemos del data persistente del jugador
            String key = "sexmod:GirlSpecific" + type.name();
            String val = sender.getPersistentData().getString(key);

            if (!val.isEmpty()) {
                result.put(type, val);
            }
        }

        // Respondemos al cliente con los datos encontrados
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), new ModelListPacket(result));
    }

    /** Lógica en el Cliente: Abre la pantalla de selección. */
    @OnlyIn(Dist.CLIENT)
    private static void handleClientSide(ModelListPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        // Usamos el mapa recibido para abrir la pantalla de selección de chicas
        mc.setScreen(new NpcTypeSelectScreen(msg.overrides));
    }
}