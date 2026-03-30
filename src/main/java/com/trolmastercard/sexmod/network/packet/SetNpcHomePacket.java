package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * SetNpcHomePacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Actualiza la posición de "hogar" de todos los NPCs que comparten un mismo dueño.
 * * La coordenada Y se redondea hacia abajo (floor) para asegurar que queden a nivel del suelo.
 */
public class SetNpcHomePacket {

    public final UUID masterUUID;
    public final Vec3 homePos;

    // ── Constructores ────────────────────────────────────────────────────────

    public SetNpcHomePacket(UUID masterUUID, Vec3 homePos) {
        this.masterUUID = masterUUID;
        this.homePos = homePos;
    }

    // ── Codec (Nativo 1.20.1) ────────────────────────────────────────────────

    public static void encode(SetNpcHomePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.masterUUID); // Nativo y eficiente
        buf.writeDouble(msg.homePos.x);
        buf.writeDouble(msg.homePos.y);
        buf.writeDouble(msg.homePos.z);
    }

    public static SetNpcHomePacket decode(FriendlyByteBuf buf) {
        return new SetNpcHomePacket(
                buf.readUUID(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
        );
    }

    // ── Manejador (Servidor) ─────────────────────────────────────────────────

    public static void handle(SetNpcHomePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // enqueueWork YA ejecuta esto en el hilo principal del servidor de forma segura
        ctx.enqueueWork(() -> {
            List<BaseNpcEntity> npcs = BaseNpcEntity.getAllWithMaster(msg.masterUUID);
            if (npcs.isEmpty()) return;

            // Redondeamos la Y hacia abajo para que la casa quede a nivel de bloque
            Vec3 snapped = new Vec3(msg.homePos.x, Math.floor(msg.homePos.y), msg.homePos.z);

            for (BaseNpcEntity npc : npcs) {
                npc.setHomePosition(snapped);
            }
        });

        ctx.setPacketHandled(true);
    }
}