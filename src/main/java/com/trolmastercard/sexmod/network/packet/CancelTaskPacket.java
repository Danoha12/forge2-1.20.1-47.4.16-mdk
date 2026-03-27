package com.trolmastercard.sexmod.network.packet;

import com.trolmastercard.sexmod.network.ModNetwork;
import com.trolmastercard.sexmod.tribe.TribeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CancelTaskPacket - ported from au.class (Fapcraft 1.12.2 v1.1) to 1.20.1.
 *
 * Sent CLIENT - SERVER when a player clicks to cancel a tribe task at a
 * specific block position.
 *
 * The server:
 *  1. Resolves the tribe UUID from the sending player
 *  2. Finds the task whose target-block set contains {@code pos}
 *  3. Cancels that task via {@link TribeManager#getTaskBlocksAt}
 *  4. Sends a {@link TribeHighlightPacket} back to the player to un-highlight
 *     the blocks that were part of the cancelled task
 */
public class CancelTaskPacket {

    private final BlockPos pos;
    private final boolean  valid;

    // =========================================================================
    //  Constructors
    // =========================================================================

    public CancelTaskPacket(BlockPos pos) {
        this.pos   = pos;
        this.valid = true;
    }

    // =========================================================================
    //  Codec
    // =========================================================================

    public static CancelTaskPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        return new CancelTaskPacket(pos);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
    }

    // =========================================================================
    //  Handler
    // =========================================================================

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                System.out.println("received an invalid Message @CancelTask :(");
                return;
            }

            UUID tribeId = TribeManager.getTribeIdForMaster(player.getUUID());
            if (tribeId == null) return;

            // Cancel the task and get back the blocks that were part of it
            Set<BlockPos> cancelledBlocks = TribeManager.getTaskBlocksAt(tribeId, pos);
            if (cancelledBlocks.isEmpty()) return;

            // Un-highlight those blocks on the client
            ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TribeHighlightPacket(new HashSet<>(cancelledBlocks), false));
        });
        ctx.setPacketHandled(true);
    }
}
