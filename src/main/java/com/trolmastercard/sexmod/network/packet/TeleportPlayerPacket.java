package com.trolmastercard.sexmod.network.packet; // Ajusta a tu paquete de red

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * TeleportPlayerPacket — Portado a 1.20.1.
 * * CLIENTE -> SERVIDOR.
 * * Teletransporta a un jugador específico a una posición y rotación exactas.
 * * Usado por el sistema de animaciones para bloquear el punto de vista del jugador.
 */
public class TeleportPlayerPacket {

    public final UUID playerUUID;
    public final Vec3 pos;
    public final float yaw;
    public final float pitch;

    // ── Constructores ────────────────────────────────────────────────────────

    public TeleportPlayerPacket(UUID playerUUID, Vec3 pos) {
        this(playerUUID, pos, 0.0F, 0.0F);
    }

    public TeleportPlayerPacket(UUID playerUUID, Vec3 pos, float yaw, float pitch) {
        this.playerUUID = playerUUID;
        this.pos = pos;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public TeleportPlayerPacket(UUID playerUUID, double x, double y, double z, float yaw, float pitch) {
        this(playerUUID, new Vec3(x, y, z), yaw, pitch);
    }

    // ── Codec (Optimizado para 1.20.1) ───────────────────────────────────────

    public static void encode(TeleportPlayerPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerUUID); // ¡Nativo y rápido!
        buf.writeDouble(msg.pos.x);
        buf.writeDouble(msg.pos.y);
        buf.writeDouble(msg.pos.z);
        buf.writeFloat(msg.yaw);
        buf.writeFloat(msg.pitch);
    }

    public static TeleportPlayerPacket decode(FriendlyByteBuf buf) {
        return new TeleportPlayerPacket(
                buf.readUUID(),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                buf.readFloat(),
                buf.readFloat()
        );
    }

    // ── Manejador ────────────────────────────────────────────────────────────

    public static void handle(TeleportPlayerPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getDirection().getReceptionSide().isClient()) {
            System.out.println("[SexMod] Error: El cliente recibió un @TeleportPlayer de forma inesperada.");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            try {
                // Buscamos al jugador directamente por su UUID
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayer(msg.playerUUID);

                if (player == null) {
                    System.out.println("[SexMod] Error: No se pudo encontrar al jugador con UUID: " + msg.playerUUID);
                    return;
                }

                float clampedYaw = Mth.wrapDegrees(msg.yaw);
                float clampedPitch = Mth.wrapDegrees(msg.pitch);

                // Forzamos las coordenadas
                player.moveTo(msg.pos.x, msg.pos.y, msg.pos.z, clampedYaw, clampedPitch);
                player.setYHeadRot(clampedYaw);
                player.setDeltaMovement(0, 0, 0);

                // Enviamos el paquete oficial de posición-mirada sin flags relativos (todo absoluto)
                player.connection.teleport(
                        msg.pos.x, msg.pos.y, msg.pos.z,
                        clampedYaw, clampedPitch,
                        Set.of()
                );

            } catch (Exception e) {
                System.err.println("[SexMod] Error al teletransportar: " + e.getMessage());
            }
        });

        ctx.setPacketHandled(true);
    }
}