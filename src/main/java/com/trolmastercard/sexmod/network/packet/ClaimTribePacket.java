package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.KoboldEntity;
import com.trolmastercard.sexmod.tribe.TribeManager;
import com.trolmastercard.sexmod.util.KoboldColorVariant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * ClaimTribePacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Registra el nombre de una tribu, asigna al dueño y lo anuncia globalmente.
 */
public class ClaimTribePacket {

    private final UUID tribeUUID;
    private final UUID playerUUID;
    private final String tribeName;

    public ClaimTribePacket(UUID tribeUUID, UUID playerUUID, String tribeName) {
        this.tribeUUID = tribeUUID;
        this.playerUUID = playerUUID;
        this.tribeName = tribeName;
    }

    // ── Codec (Optimizado) ───────────────────────────────────────────────────

    public static void encode(ClaimTribePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.tribeUUID);
        buf.writeUUID(msg.playerUUID);
        buf.writeUtf(msg.tribeName);
    }

    public static ClaimTribePacket decode(FriendlyByteBuf buf) {
        return new ClaimTribePacket(buf.readUUID(), buf.readUUID(), buf.readUtf());
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(ClaimTribePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (!ctx.getDirection().getReceptionSide().isServer()) return;

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            // Validación de seguridad: el sender debe ser quien dice ser
            if (sender == null || !sender.getUUID().equals(msg.playerUUID)) {
                System.out.println("Intento de reclamo de tribu inválido o malicioso detectado.");
                return;
            }

            // Obtenemos todos los miembros de la tribu a través del TribeManager
            List<KoboldEntity> members = TribeManager.getKoboldList(msg.tribeUUID);
            KoboldColorVariant tribeColor = null;

            for (KoboldEntity kobold : members) {
                if (kobold.isRemoved()) continue;

                // Actualizar datos de red sincronizados
                kobold.getEntityData().set(BaseNpcEntity.MASTER_UUID, msg.playerUUID.toString());
                kobold.getEntityData().set(KoboldEntity.DATA_TRIBE_NAME, msg.tribeName);

                // Determinar el color de la tribu basado en el primer Kobold encontrado
                if (tribeColor == null) {
                    String colorStr = kobold.getEntityData().get(KoboldEntity.DATA_COLOR);
                    tribeColor = KoboldColorVariant.fromString(colorStr);
                }
            }

            if (tribeColor == null) return;

            // ── Anuncio Global ───────────────────────────────────────────────

            // Construir el mensaje con el sistema de componentes moderno
            MutableComponent annuncement = Component.literal(sender.getName().getString())
                    .append(" formed the ")
                    .append(Component.literal(msg.tribeName).withStyle(tribeColor.getChatStyle()))
                    .append(" Tribe");

            // Enviar a todos los jugadores online
            sender.server.getPlayerList().broadcastSystemMessage(annuncement, false);

            // Registrar datos persistentes en el servidor
            TribeManager.setTribeActive(msg.tribeUUID, true);
            TribeManager.setTribeOwner(msg.tribeUUID, sender.getUUID());
        });

        ctx.setPacketHandled(true);
    }
}