package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * DespawnClothingPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Elimina las entidades vinculadas a un dueño específico.
 * * Se usa principalmente cuando Allie "regresa" a su lámpara.
 */
public class DespawnClothingPacket {

    private final UUID ownerUuid;

    public DespawnClothingPacket(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    // ── Codec (Optimizado para UUIDs) ────────────────────────────────────────

    public static DespawnClothingPacket decode(FriendlyByteBuf buf) {
        // En 1.20.1 es mejor usar readUUID() directamente
        return new DespawnClothingPacket(buf.readUUID());
    }

    public static void encode(DespawnClothingPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.ownerUuid);
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(DespawnClothingPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // Buscamos a todas las chicas que pertenecen a este dueño
            // Usamos el método que definimos en BaseNpcEntity
            List<BaseNpcEntity> npcs = BaseNpcEntity.getByOwner(msg.ownerUuid);

            for (BaseNpcEntity npc : npcs) {
                // DISCARDED es el motivo correcto para cuando una entidad
                // simplemente "deja de existir" sin morir (como Allie volviendo a la lámpara)
                npc.remove(Entity.RemovalReason.DISCARDED);
            }
        });
        ctx.setPacketHandled(true);
    }
}