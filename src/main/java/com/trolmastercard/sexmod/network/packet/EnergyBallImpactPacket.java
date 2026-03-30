package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.entity.EnergyBallEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * EnergyBallImpactPacket — Nuevo para 1.20.1.
 * * SERVIDOR -> CLIENTE.
 * * Le dice al cliente que dibuje partículas en una coordenada exacta (Vec3).
 * * Se usa cuando la bola de energía choca (invocación) o es destruida.
 */
public class EnergyBallImpactPacket {

    private final Vec3 pos;
    private final boolean isSummoning;

    public EnergyBallImpactPacket(Vec3 pos, boolean isSummoning) {
        this.pos = pos;
        this.isSummoning = isSummoning;
    }

    // ── Codec ────────────────────────────────────────────────────────────────

    public static void encode(EnergyBallImpactPacket msg, FriendlyByteBuf buf) {
        // Desarmamos el Vec3 en 3 Doubles
        buf.writeDouble(msg.pos.x);
        buf.writeDouble(msg.pos.y);
        buf.writeDouble(msg.pos.z);
        buf.writeBoolean(msg.isSummoning);
    }

    public static EnergyBallImpactPacket decode(FriendlyByteBuf buf) {
        // Lo volvemos a armar al recibirlo
        return new EnergyBallImpactPacket(
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readBoolean()
        );
    }

    // ── Manejador (Handler) ──────────────────────────────────────────────────

    public static void handle(EnergyBallImpactPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> handleClient(msg));
        }
        ctx.setPacketHandled(true);
    }

    /**
     * Lógica asilada para el Cliente.
     */
    private static void handleClient(EnergyBallImpactPacket msg) {
        if (msg.isSummoning) {
            // Efecto de anillo de humo para cuando aparece el Wither Skeleton
            EnergyBallEntity.spawnRingParticles(msg.pos);
        } else {
            // Efecto para cuando una flecha destruye la bola
            // (Reusamos el anillo de humo, pero podrías meter otras partículas aquí)
            EnergyBallEntity.spawnRingParticles(msg.pos);
        }
    }
}