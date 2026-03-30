package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CatThrowAwayItemPacket — Portado a 1.20.1.
 * * CLIENTE → SERVIDOR.
 * * Hace que todos los NPCs vinculados al UUID tiren el ítem que sostienen.
 * * Se activa principalmente con las animaciones de Luna/Cat.
 */
public class CatThrowAwayItemPacket {

    private final UUID ownerUUID;

    public CatThrowAwayItemPacket(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    // ── Codec (Optimizado con UUID nativo) ───────────────────────────────────

    public static void encode(CatThrowAwayItemPacket msg, FriendlyByteBuf buf) {
        // En 1.20.1 es mejor usar writeUUID que convertirlo a String
        buf.writeUUID(msg.ownerUUID);
    }

    public static CatThrowAwayItemPacket decode(FriendlyByteBuf buf) {
        return new CatThrowAwayItemPacket(buf.readUUID());
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(CatThrowAwayItemPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Validamos que el paquete llegue al servidor
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            return;
        }

        ctx.enqueueWork(() -> {
            // Buscamos todos los NPCs asociados al jugador
            List<BaseNpcEntity> npcs = BaseNpcEntity.getByOwner(msg.ownerUUID);

            for (BaseNpcEntity npc : npcs) {
                // Solo procesamos en el servidor y si es una entidad con inventario
                if (!npc.level().isClientSide() && npc instanceof NpcInventoryEntity inventoryNpc) {
                    // Este método debe estar definido en NpcInventoryEntity
                    // o en la clase específica del NPC (como CatPlayerKobold)
                    inventoryNpc.throwAwayCurrentItem();
                }
            }
        });

        ctx.setPacketHandled(true);
    }
}