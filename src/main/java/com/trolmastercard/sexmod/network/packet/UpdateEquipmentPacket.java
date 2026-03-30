package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.NpcInventoryEntity; // Asumo que esta clase existe
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * UpdateEquipmentPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Actualiza el NBT del ItemStackHandler del equipo de los NPCs del jugador.
 * * 🚨 SEGURIDAD MEJORADA: El UUID se lee del remitente verificado por el servidor, no del paquete.
 */
public class UpdateEquipmentPacket {

    public final CompoundTag equipmentTag;

    public UpdateEquipmentPacket(CompoundTag equipmentTag) {
        this.equipmentTag = equipmentTag;
    }

    // ── Codec (Optimizado y Seguro) ──────────────────────────────────────────

    public static void encode(UpdateEquipmentPacket msg, FriendlyByteBuf buf) {
        // Adiós a la conversión de UUID a String. ¡Solo mandamos el NBT!
        buf.writeNbt(msg.equipmentTag);
    }

    public static UpdateEquipmentPacket decode(FriendlyByteBuf buf) {
        return new UpdateEquipmentPacket(buf.readNbt());
    }

    // ── Manejador (Servidor) ─────────────────────────────────────────────────

    public static void handle(UpdateEquipmentPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Chequeo de seguridad: asegurar que solo el servidor procese esto
        if (ctx.getDirection().getReceptionSide().isClient()) {
            System.out.println("[SexMod] Error: El cliente recibió un @UpdateEquipment de forma inesperada.");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // 🛡️ ANTI-CHEAT: Usamos el UUID del remitente real, ignorando lo que diga el cliente
            List<BaseNpcEntity> owned = BaseNpcEntity.getAllByOwnerUUID(sender.getUUID());

            for (BaseNpcEntity npc : owned) {
                // Pattern matching moderno de Java 16+
                if (npc instanceof NpcInventoryEntity inv) {
                    if (msg.equipmentTag != null) {
                        inv.equipmentHandler.deserializeNBT(msg.equipmentTag);
                    }
                }
            }
        });

        ctx.setPacketHandled(true);
    }
}