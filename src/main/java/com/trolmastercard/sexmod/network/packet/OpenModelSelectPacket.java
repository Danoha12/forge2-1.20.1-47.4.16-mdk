package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

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
 * OpenModelSelectPacket — Portado a 1.20.1.
 * * Paquete Bidireccional:
 * * C -> S: Pide al servidor las preferencias guardadas en el NBT del jugador.
 * * S -> C: Envía el mapa de modelos y le ordena al cliente abrir la interfaz.
 */
public class OpenModelSelectPacket {

    public static final String NBT_PREFIX = "sexmod:GirlSpecific";

    private final HashMap<NpcType, String> modelOverrides;

    // ── Constructores ────────────────────────────────────────────────────────

    /** Constructor C -> S: El cliente envía un paquete vacío solo para pedir datos */
    public OpenModelSelectPacket() {
        this.modelOverrides = new HashMap<>();
    }

    /** Constructor S -> C: El servidor envía los datos encontrados en el NBT */
    public OpenModelSelectPacket(HashMap<NpcType, String> overrides) {
        this.modelOverrides = overrides;
    }

    // ── Codec (Serialización pura, sin lógica de entidades) ──────────────────

    public static void encode(OpenModelSelectPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.modelOverrides.size());
        for (Map.Entry<NpcType, String> entry : msg.modelOverrides.entrySet()) {
            // 1.20.1: Escribimos el Enum de forma nativa
            buf.writeEnum(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static OpenModelSelectPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        HashMap<NpcType, String> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            NpcType type = buf.readEnum(NpcType.class);
            String model = buf.readUtf();
            map.put(type, model);
        }
        return new OpenModelSelectPacket(map);
    }

    // ── Manejador Bidireccional ──────────────────────────────────────────────

    public static void handle(OpenModelSelectPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // 1. LÓGICA DEL SERVIDOR (El cliente nos pide sus datos)
        if (ctx.getDirection().getReceptionSide().isServer()) {
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player == null) return;

                HashMap<NpcType, String> toSend = new HashMap<>();
                for (NpcType type : NpcType.values()) {
                    if (!type.hasSpecifics) continue;

                    // Leemos el NBT persistente del jugador
                    String val = player.getPersistentData().getString(NBT_PREFIX + type.name());
                    if (!val.isEmpty()) {
                        toSend.put(type, val);
                    }
                }

                // Disparamos el paquete de vuelta hacia el cliente con los datos
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenModelSelectPacket(toSend));
            });
        }
        // 2. LÓGICA DEL CLIENTE (El servidor nos manda los datos listos)
        else {
            ctx.enqueueWork(() -> {
                openScreenOnClient(msg.modelOverrides);
            });
        }
        ctx.setPacketHandled(true);
    }

    // ── Apertura de GUI aislada ──────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    private static void openScreenOnClient(HashMap<NpcType, String> overrides) {
        Minecraft mc = Minecraft.getInstance();
        // En 1.20.1, setScreen es seguro para llamar en el cliente directamente
        mc.setScreen(new NpcTypeSelectScreen(overrides));
    }
}