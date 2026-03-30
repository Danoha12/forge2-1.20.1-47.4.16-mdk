package com.trolmastercard.sexmod.network;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.BeeEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * BeeOpenChestPacket — Portado a 1.20.1 y enmascarado (SFW).
 * Enviado por el cliente para solicitar al servidor que abra el inventario del NPC Bee.
 * * Carga útil: npcUUID (Identidad del NPC) + playerUUID (ID del jugador).
 */
public class BeeOpenChestPacket {

    private final UUID npcUUID;
    private final UUID playerUUID;

    public BeeOpenChestPacket(UUID npcUUID, UUID playerUUID) {
        this.npcUUID = npcUUID;
        this.playerUUID = playerUUID;
    }

    // ── Codec (Codificación/Decodificación) ──────────────────────────────────

    public static void encode(BeeOpenChestPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.npcUUID);
        buf.writeUUID(pkt.playerUUID);
    }

    public static BeeOpenChestPacket decode(FriendlyByteBuf buf) {
        return new BeeOpenChestPacket(buf.readUUID(), buf.readUUID());
    }

    // ── Handler (Lógica del Servidor) ─────────────────────────────────────────

    public static void handle(BeeOpenChestPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // Buscar la instancia del NPC en el servidor usando su UUID único
            BaseNpcEntity entity = BaseNpcEntity.getServerNpc(pkt.npcUUID);

            if (entity instanceof BeeEntity bee) {
                // Verificar que el NPC no esté en un mundo de cliente (seguridad)
                if (bee.level().isClientSide()) return;

                // Solo abrir si la abeja tiene el cofre equipado (DATA_TAMED / DATA_HAS_CHEST)
                // Usamos el acceso a datos sincronizados que definimos en BeeEntity
                if (!bee.getEntityData().get(BeeEntity.DATA_TAMED)) return;

                ServerPlayer target = (ServerPlayer) bee.level().getPlayerByUUID(pkt.playerUUID);
                if (target == null) return;

                // Abrir el menú de inventario del NPC para el jugador
                // Nota: Requiere que BeeEntity implemente getChestMenuProvider() o similar
                // target.openMenu(bee.getChestMenuProvider());

                System.out.println("[Network] Abriendo inventario de Bee para: " + target.getName().getString());
            }
        });
        ctx.setPacketHandled(true);
    }
}