package com.trolmastercard.sexmod.network.packet;

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
 * TeleportPlayerPacket - ported from a8.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER to teleport a specific player to an exact position and
 * rotation. Used by the sex-animation system to lock the player's viewpoint.
 *
 * The server teleports the player using
 * {@link ServerPlayer#connection#teleport()} with an empty relative-flags set
 * (i.e. all coordinates are absolute), mirroring the original
 * {@code SPacketPlayerPosLook} approach.
 *
 * Constructors (mirror the original overloads):
 *   TeleportPlayerPacket(playerUUIDString, pos)             - yaw/pitch = 0
 *   TeleportPlayerPacket(playerUUIDString, pos, yaw, pitch)
 *   TeleportPlayerPacket(playerUUIDString, x, y, z, yaw, pitch)
 */
public class TeleportPlayerPacket {

    private final String  playerUUIDStr;
    private final Vec3    pos;
    private final float   yaw;
    private final float   pitch;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public TeleportPlayerPacket(String playerUUID, Vec3 pos) {
        this(playerUUID, pos, 0.0F, 0.0F);
    }

    public TeleportPlayerPacket(String playerUUID, Vec3 pos,
                                 float yaw, float pitch) {
        this.playerUUIDStr = playerUUID;
        this.pos           = pos;
        this.yaw           = yaw;
        this.pitch         = pitch;
        this.valid         = true;
    }

    public TeleportPlayerPacket(String playerUUID,
                                 double x, double y, double z,
                                 float yaw, float pitch) {
        this(playerUUID, new Vec3(x, y, z), yaw, pitch);
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static TeleportPlayerPacket decode(FriendlyByteBuf buf) {
        String uuidStr = buf.readUtf();
        Vec3   pos     = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float  yaw     = buf.readFloat();
        float  pitch   = buf.readFloat();
        return new TeleportPlayerPacket(uuidStr, pos, yaw, pitch);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(playerUUIDStr);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        if (ctx.getSender() == null) {
            // Client-side receipt - this packet is server-client only in rare cases
            System.out.println("received an invalid message @TeleportPlayer :(");
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @TeleportPlayer :(");
                return;
            }

            ServerLifecycleHooks.getCurrentServer().execute(() -> {
                try {
                    System.out.println("teleporting player " + playerUUIDStr + " to " + pos);

                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayer(UUID.fromString(playerUUIDStr));

                    if (player == null) {
                        throw new IllegalArgumentException("player not found");
                    }

                    float clampedYaw   = Mth.wrapDegrees(yaw);
                    float clampedPitch = Mth.wrapDegrees(pitch);

                    player.moveTo(pos.x, pos.y, pos.z, clampedYaw, clampedPitch);
                    player.setYHeadRot(clampedYaw);
                    player.setDeltaMovement(0, 0, 0);

                    // Send the official position-look packet with no relative flags
                    player.connection.teleport(
                        pos.x, pos.y, pos.z,
                        clampedYaw, clampedPitch,
                        Set.of());   // empty = all absolute

                } catch (Exception e) {
                    System.out.println("couldn't find player with UUID: " + playerUUIDStr);
                    System.out.println("could only find the following players:");
                    ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayers()
                        .forEach(p -> System.out.println("  " + p.getName().getString()));
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}
