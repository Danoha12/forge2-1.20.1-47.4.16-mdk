package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.entity.GalathEntity; // Asegúrate de tener esta clase
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * UpdateGalathVelocityPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Actualiza el offset de posición/velocidad de Galath durante una secuencia.
 * * Validado para que solo el compañero actual pueda enviar las actualizaciones.
 */
public class UpdateGalathVelocityPacket {

    public final Vec3 pos;
    public final UUID npcUUID;

    public UpdateGalathVelocityPacket(Vec3 pos, UUID npcUUID) {
        this.pos = pos;
        this.npcUUID = npcUUID;
    }

    // ── Codec (¡Libre de Strings!) ───────────────────────────────────────────

    public static void encode(UpdateGalathVelocityPacket msg, FriendlyByteBuf buf) {
        buf.writeDouble(msg.pos.x);
        buf.writeDouble(msg.pos.y);
        buf.writeDouble(msg.pos.z);
        buf.writeUUID(msg.npcUUID); // Nativo y ultra rápido
    }

    public static UpdateGalathVelocityPacket decode(FriendlyByteBuf buf) {
        return new UpdateGalathVelocityPacket(
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readUUID()
        );
    }

    // ── Manejador (Servidor) ─────────────────────────────────────────────────

    public static void handle(UpdateGalathVelocityPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Chequeo temprano de lado de red
        if (ctx.getDirection().getReceptionSide().isClient()) {
            System.out.println("[SexMod] Error: El cliente recibió un @UpdateVelocity de forma inesperada.");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;

            // Buscar a la entidad por su UUID
            BaseNpcEntity npc = BaseNpcEntity.getNpcByUUID(msg.npcUUID);

            // Pattern matching moderno de Java 16+
            if (!(npc instanceof GalathEntity galath)) return;

            // Seguridad: Solo permitimos la actualización si el remitente es el compañero actual.
            // Comparamos por UUID en lugar de .equals() para evitar problemas de desincronización de instancias.
            if (galath.getSexPartner() != null && sender.getUUID().equals(galath.getSexPartner().getUUID())) {
                galath.updateSexVelocity(msg.pos);
            } else {
                System.out.println("[SexMod] Advertencia: Jugador " + sender.getName().getString() +
                        " intentó mover a Galath sin ser su compañero.");
            }
        });

        ctx.setPacketHandled(true);
    }
}