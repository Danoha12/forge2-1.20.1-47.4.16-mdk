package com.trolmastercard.sexmod.network.packet;
import com.trolmastercard.sexmod.BaseNpcEntity;

import com.trolmastercard.sexmod.entity.BaseNpcEntity;
import com.trolmastercard.sexmod.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * ResetControllerPacket - ported from a1.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Bidirectional packet (CLIENT-SERVER and SERVER-CLIENT).
 *
 * CLIENT-SERVER: player requests to reset the "ticksPlaying" counter on a
 *   specific NPC. The server resets it and then re-broadcasts this packet to
 *   all players within 100 blocks of the NPC (except the sender).
 *
 * SERVER-CLIENT: client receives the broadcast and calls {@code npc.resetController()}
 *   (i.e. {@code em.ag()} in the original) on the matching local NPC instance.
 *
 * Range constant: 100 blocks (b = 100 in the original).
 */
public class ResetControllerPacket {

    private static final float BROADCAST_RANGE = 100.0F;

    private final UUID    npcUUID;
    private final boolean valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public ResetControllerPacket(UUID npcUUID) {
        this.npcUUID = npcUUID;
        this.valid   = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static ResetControllerPacket decode(FriendlyByteBuf buf) {
        UUID uuid = UUID.fromString(buf.readUtf());
        return new ResetControllerPacket(uuid);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(npcUUID.toString());
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (!valid) {
                System.out.println("received an invalid message @ResetController :(");
                return;
            }

            ServerPlayer sender = ctx.getSender();

            if (sender != null) {
                // ---- SERVER SIDE ----
                BaseNpcEntity npc = BaseNpcEntity.getByIdServer(npcUUID);
                if (npc == null) return;

                // Reset the animation controller tick counter
                npc.getAnimController().ticksPlaying = new int[]{ 0, 0 };

                UUID senderUUID = sender.getUUID();

                // Re-broadcast to all players within range (except sender)
                for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer()
                        .getPlayerList().getPlayers()) {
                    if (senderUUID.equals(player.getUUID())) continue;
                    if (player.distanceTo((Entity) npc) < BROADCAST_RANGE) {
                        ModNetwork.CHANNEL.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new ResetControllerPacket(npcUUID));
                    }
                }

            } else {
                // ---- CLIENT SIDE ----
                BaseNpcEntity npc = BaseNpcEntity.getByIdClient(npcUUID);
                if (npc != null) {
                    npc.resetController(); // em.ag()
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
