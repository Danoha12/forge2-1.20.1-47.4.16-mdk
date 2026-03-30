package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.world.inventory.NpcInventoryMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * OpenNpcInventoryPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Solicita abrir el contenedor de inventario de un NPC específico.
 */
public class OpenNpcInventoryPacket {

    private final UUID npcUUID;

    public OpenNpcInventoryPacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
    }

    // ── Codec (Optimizado) ───────────────────────────────────────────────────

    public static void encode(OpenNpcInventoryPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.npcUUID);
    }

    public static OpenNpcInventoryPacket decode(FriendlyByteBuf buf) {
        return new OpenNpcInventoryPacket(buf.readUUID());
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(OpenNpcInventoryPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Solo procesamos en el servidor
        if (!ctx.getDirection().getReceptionSide().isServer()) return;

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // Buscamos al NPC en la lista de activos usando su UUID
            BaseNpcEntity targetNpc = null;
            for (BaseNpcEntity npc : BaseNpcEntity.getAllActive()) {
                if (!npc.isRemoved() && npc.getUUID().equals(msg.npcUUID)) {
                    targetNpc = npc;
                    break;
                }
            }

            // Si encontramos al NPC, abrimos el menú
            if (targetNpc != null) {
                // NpcInventoryMenuProvider debe implementar MenuProvider (antes IInventory)
                player.openMenu(new NpcInventoryMenuProvider(targetNpc));
            }
        });

        ctx.setPacketHandled(true);
    }
}