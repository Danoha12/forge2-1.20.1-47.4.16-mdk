package com.trolmastercard.sexmod.network.packet;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * SetPlayerCamPacket — Portado a 1.20.1.
 * * SERVIDOR → CLIENTE.
 * * Fuerza la cámara del jugador a un ángulo (pitch/yaw) y perspectiva específicos.
 * * Seguro para servidores dedicados (Lógica de cliente aislada).
 */
public class SetPlayerCamPacket {

    private final float pitch;
    private final float yaw;
    private final int perspective; // 0=Primera, 1=Tercera Atrás, 2=Tercera Frente

    public SetPlayerCamPacket(float pitch, float yaw, int perspective) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.perspective = perspective;
    }

    // ── Serialización ─────────────────────────────────────────────────────────

    public static void encode(SetPlayerCamPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.pitch);
        buf.writeFloat(msg.yaw);
        buf.writeInt(msg.perspective);
    }

    public static SetPlayerCamPacket decode(FriendlyByteBuf buf) {
        return new SetPlayerCamPacket(
                buf.readFloat(),
                buf.readFloat(),
                buf.readInt()
        );
    }

    // ── Manejador (Handler) ───────────────────────────────────────────────────

    public static void handle(SetPlayerCamPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Validamos que el paquete llegue al Cliente (Si llega al Servidor, lo ignoramos)
        if (ctx.getDirection().getReceptionSide().isClient()) {
            // enqueueWork ya ejecuta el código en el hilo principal del juego (Minecraft.getInstance())
            ctx.enqueueWork(() -> handleClientSide(msg));
        } else {
            System.out.println("[SexMod] Paquete SetPlayerCamPacket recibido en el lado equivocado.");
        }

        ctx.setPacketHandled(true);
    }

    /**
     * Aislamiento Crítico: Este método solo será leído por el ClassLoader si estamos en el Cliente.
     */
    private static void handleClientSide(SetPlayerCamPacket msg) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // 1. Configurar la perspectiva de la cámara
        CameraType[] types = CameraType.values();
        if (msg.perspective >= 0 && msg.perspective < types.length) {
            mc.options.setCameraType(types[msg.perspective]);
        }

        // 2. Aplicar los ángulos a absolutamente todos los rastreadores de rotación del jugador
        player.setYRot(msg.yaw);
        player.yRotO = msg.yaw;
        player.yHeadRot = msg.yaw;
        player.yHeadRotO = msg.yaw;
        player.yBodyRot = msg.yaw;
        player.yBodyRotO = msg.yaw; // Importante para que el torso no gire raro en el primer frame

        player.setXRot(msg.pitch);
        player.xRotO = msg.pitch;
    }
}