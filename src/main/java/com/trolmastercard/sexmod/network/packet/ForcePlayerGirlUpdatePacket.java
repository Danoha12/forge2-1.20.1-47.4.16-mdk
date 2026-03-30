package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.PlayerKoboldEntity;
import com.trolmastercard.sexmod.registry.AnimState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * ForcePlayerGirlUpdatePacket — Portado a 1.20.1.
 * * SERVIDOR -> CLIENTE.
 * * Fuerza una actualización instantánea del AnimState y el subtipo de la chica
 * * sin esperar al tick de sincronización del EntityData natural.
 */
public class ForcePlayerGirlUpdatePacket {

    private final UUID playerUUID;
    private final int subtype;
    private final AnimState animState;

    public ForcePlayerGirlUpdatePacket(UUID playerUUID, int subtype, AnimState animState) {
        this.playerUUID = playerUUID;
        this.subtype = subtype;
        this.animState = animState;
    }

    // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

    public static void encode(ForcePlayerGirlUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID); // Nativo y más rápido que un String
        buf.writeInt(msg.subtype);
        buf.writeEnum(msg.animState);  // Serialización nativa de Enums
    }

    public static ForcePlayerGirlUpdatePacket decode(FriendlyByteBuf buf) {
        return new ForcePlayerGirlUpdatePacket(
                buf.readUUID(),
                buf.readInt(),
                buf.readEnum(AnimState.class)
        );
    }

    // ── Manejador (Handler Seguro) ───────────────────────────────────────────

    public static void handle(ForcePlayerGirlUpdatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> handleClient(msg));
        }
        ctx.setPacketHandled(true);
    }

    /**
     * Lógica aislada para el Cliente.
     * Evita el infame ClassNotFoundException en Servidores Dedicados.
     */
    private static void handleClient(ForcePlayerGirlUpdatePacket msg) {
        PlayerKoboldEntity player = PlayerKoboldEntity.getByUUIDClient(msg.playerUUID);

        if (player == null) {
            System.out.println("[SexMod] Error: Mensaje ForcePlayerGirlUpdate recibido, pero la entidad no existe en el cliente.");
            return;
        }

        // Forzamos la actualización directa de los DataParameters en el cliente
        player.getEntityData().set(BaseNpcEntity.DATA_ANIM_STATE, msg.animState.name());
        player.getEntityData().set(BaseNpcEntity.DATA_SUBTYPE, msg.subtype);
    }
}