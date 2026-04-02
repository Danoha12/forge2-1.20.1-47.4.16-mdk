package com.trolmastercard.sexmod.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * AcceptSexProposalPacket — Envía la confirmación al servidor de que el jugador aceptó la interacción.
 */
public class AcceptSexProposalPacket {

    private final UUID requesterUUID;
    private final UUID targetUUID;
    private final String actTranslationKey;

    public AcceptSexProposalPacket(UUID requesterUUID, UUID targetUUID, String actTranslationKey) {
        this.requesterUUID = requesterUUID;
        this.targetUUID = targetUUID;
        this.actTranslationKey = actTranslationKey;
    }

    public static void encode(AcceptSexProposalPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.requesterUUID);
        buf.writeUUID(msg.targetUUID);
        buf.writeUtf(msg.actTranslationKey);
    }

    public static AcceptSexProposalPacket decode(FriendlyByteBuf buf) {
        return new AcceptSexProposalPacket(
                buf.readUUID(),
                buf.readUUID(),
                buf.readUtf()
        );
    }

    public static void handle(AcceptSexProposalPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Lógica del servidor:
            // 1. Buscar al jugador 'requester' y al jugador 'target'.
            // 2. Verificar que ambos sigan cerca y válidos.
            // 3. Iniciar la animación/entidad de interacción correspondiente usando el actTranslationKey.
        });
        ctx.get().setPacketHandled(true);
    }
}