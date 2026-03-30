package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.client.gui.InteractionMeterOverlay;
import com.trolmastercard.sexmod.client.handler.ClientStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * CameraControlPacket — Portado a 1.20.1.
 * * SERVIDOR → CLIENTE.
 * * Bloquea la cámara, el movimiento y muestra el "Interaction Meter" (GUI).
 */
public class CameraControlPacket {

    private final boolean locked;

    public CameraControlPacket(boolean locked) {
        this.locked = locked;
    }

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(CameraControlPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.locked);
    }

    public static CameraControlPacket decode(FriendlyByteBuf buf) {
        return new CameraControlPacket(buf.readBoolean());
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(CameraControlPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Verificamos que el paquete se recibió en el Cliente
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                // Llamamos al método aislado para evitar crasheos en servidores
                handleClientLogic(msg);
            });
        }
        ctx.setPacketHandled(true);
    }

    /**
     * Lógica exclusiva del Cliente.
     * Usamos @OnlyIn por seguridad adicional del compilador.
     */
    // [El resto del código que mandaste está perfecto, solo un detalle en el handler]

    @OnlyIn(Dist.CLIENT)
    private static void handleClientLogic(CameraControlPacket msg) {
        // 1. Guardamos el estado global
        ClientStateManager.setPlayerLocked(msg.locked);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 2. Frenazo en seco
            mc.player.setDeltaMovement(0, 0, 0);

            // 3. UI y Cámara
            if (msg.locked) {
                InteractionMeterOverlay.show();
                // Opcional: Forzar tercera persona al empezar
                // mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            } else {
                InteractionMeterOverlay.hide();
                // Opcional: Devolver a primera persona al terminar
                // mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        }
    }
}