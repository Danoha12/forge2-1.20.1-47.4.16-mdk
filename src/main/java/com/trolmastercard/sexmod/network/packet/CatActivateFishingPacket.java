package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.LunaEntity;
import com.trolmastercard.sexmod.item.LunaRodItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * CatActivateFishingPacket — Portado a 1.20.1 y optimizado.
 * * Enviado desde el CLIENTE al SERVIDOR.
 * Activa la lógica de pesca para la entidad Luna usando su caña especial.
 */
public class CatActivateFishingPacket {

    private final UUID npcUUID;

    public CatActivateFishingPacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // ── Serialización (FriendlyByteBuf) ───────────────────────────────────────

    public static void encode(CatActivateFishingPacket msg, FriendlyByteBuf buf) {
        // En 1.20.1, escribir el UUID directamente es más eficiente que pasarlo a String
        buf.writeUUID(msg.npcUUID);
    }

    public static CatActivateFishingPacket decode(FriendlyByteBuf buf) {
        return new CatActivateFishingPacket(buf.readUUID());
    }

    // ── Manejador (Handler) ───────────────────────────────────────────────────

    public static void handle(CatActivateFishingPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Solo procesar en el servidor
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Buscamos la entidad directamente en el nivel del servidor por su UUID
            // Esto es mucho más eficiente que un bucle 'for' manual
            Entity target = player.serverLevel().getEntity(msg.npcUUID);

            if (target instanceof LunaEntity luna) {
                // Verificamos que no estemos en el cliente (seguridad extra)
                if (luna.level().isClientSide()) return;

                // Accedemos al ítem que Luna tiene "visualmente" (su caña de pescar)
                var rodStack = luna.heldVisualItem;

                if (!rodStack.isEmpty() && rodStack.getItem() instanceof LunaRodItem rod) {
                    // Disparamos la lógica de uso de la caña en el servidor
                    rod.use(luna.level(), luna, InteractionHand.MAIN_HAND);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}